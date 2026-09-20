package com.uopeople.cs4405.bugtracker.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.uopeople.cs4405.bugtracker.data.local.IssueDao
import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.PendingOperation
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.local.SyncState
import com.uopeople.cs4405.bugtracker.data.remote.IssueApi
import com.uopeople.cs4405.bugtracker.data.remote.IssueMapper
import com.uopeople.cs4405.bugtracker.worker.IssueSyncWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Repository coordinating Room (local source of truth), Retrofit, and WorkManager.
 * Enforces offline-first invariants, conflict handling, and tombstone management.
 */
class IssueRepository(
    private val issueDao: IssueDao,
    private val issueApi: IssueApi,
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Observes all active (non-deleted) issues from the local Room database.
     */
    fun observeActiveIssues(): Flow<List<IssueEntity>> = issueDao.observeActiveIssues()

    /**
     * Retrieves an issue by its UUID.
     */
    suspend fun getIssue(id: String): IssueEntity? = withContext(ioDispatcher) {
        issueDao.getIssueById(id)
    }

    /**
     * Offline-first CREATE:
     * 1. Validates title.
     * 2. Generates a UUID locally.
     * 3. Persists in Room immediately with PENDING state and CREATE operation.
     * 4. Enqueues background sync work.
     */
    suspend fun createIssue(
        title: String,
        description: String,
        priority: Priority,
        status: IssueStatus
    ): SyncResult<IssueEntity> = withContext(ioDispatcher) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return@withContext SyncResult.ValidationError("Issue title cannot be empty.")
        }

        val currentTime = System.currentTimeMillis()
        val newIssue = IssueEntity(
            id = UUID.randomUUID().toString(),
            title = trimmedTitle,
            description = description.trim(),
            priority = priority,
            status = status,
            createdAt = currentTime,
            updatedAt = currentTime,
            syncState = SyncState.PENDING,
            isDeleted = false,
            operationType = PendingOperation.CREATE
        )

        issueDao.insertIssue(newIssue)
        enqueueSyncWork()
        SyncResult.Success(newIssue)
    }

    /**
     * Offline-first UPDATE:
     * 1. Validates title.
     * 2. Updates Room immediately, updating updatedAt.
     * 3. Preserves CREATE operation if unsynced; otherwise sets UPDATE operation.
     * 4. Enqueues background sync work.
     */
    suspend fun updateIssue(
        id: String,
        title: String,
        description: String,
        priority: Priority,
        status: IssueStatus
    ): SyncResult<IssueEntity> = withContext(ioDispatcher) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return@withContext SyncResult.ValidationError("Issue title cannot be empty.")
        }

        val existing = issueDao.getIssueById(id)
            ?: return@withContext SyncResult.UnexpectedError("Issue not found: $id")

        val newOp = if (existing.operationType == PendingOperation.CREATE) {
            PendingOperation.CREATE
        } else {
            PendingOperation.UPDATE
        }

        val updated = existing.copy(
            title = trimmedTitle,
            description = description.trim(),
            priority = priority,
            status = status,
            updatedAt = System.currentTimeMillis(),
            syncState = SyncState.PENDING,
            operationType = newOp
        )

        issueDao.updateIssue(updated)
        enqueueSyncWork()
        SyncResult.Success(updated)
    }

    /**
     * Offline-first DELETE (Tombstone pattern):
     * 1. If created and deleted entirely offline (CREATE + PENDING), immediately purge without remote delete.
     * 2. Otherwise, mark logically deleted (isDeleted = true, operationType = DELETE) to preserve tombstone.
     * 3. Enqueues background sync work.
     */
    suspend fun deleteIssue(id: String): SyncResult<Unit> = withContext(ioDispatcher) {
        val existing = issueDao.getIssueById(id)
            ?: return@withContext SyncResult.UnexpectedError("Issue not found: $id")

        if (existing.operationType == PendingOperation.CREATE && existing.syncState == SyncState.PENDING) {
            // Created offline and never sent to server; delete permanently right away
            issueDao.deleteIssuePermanently(id)
        } else {
            // Preserves deletion tombstone for remote replication
            issueDao.markLogicalDelete(id)
            enqueueSyncWork()
        }
        SyncResult.Success(Unit)
    }

    /**
     * Synchronizes all pending local mutations with the remote REST API.
     * Executes pending operations sequentially to maintain chronological ordering.
     */
    suspend fun syncPendingOperations(): SyncResult<Int> = withContext(ioDispatcher) {
        val pendingList = issueDao.getPendingOperations()
        if (pendingList.isEmpty()) {
            return@withContext SyncResult.Success(0)
        }

        var syncedCount = 0

        for (issue in pendingList) {
            when (issue.operationType) {
                PendingOperation.CREATE -> {
                    try {
                        val response = issueApi.createIssue(IssueMapper.toDto(issue))
                        if (response.isSuccessful) {
                            issueDao.updateSyncState(issue.id, SyncState.SYNCED, PendingOperation.NONE)
                            syncedCount++
                        } else if (response.code() in 500..599) {
                            // Server error: keep PENDING for retry
                            return@withContext SyncResult.ServerError(response.code(), "Server temporary error")
                        } else {
                            // Client error (4xx): mark FAILED
                            issueDao.updateSyncState(issue.id, SyncState.FAILED, issue.operationType)
                        }
                    } catch (e: IOException) {
                        return@withContext SyncResult.NetworkError("Network unavailable during create sync", e)
                    } catch (e: Exception) {
                        return@withContext SyncResult.UnexpectedError("Unexpected error during create sync", e)
                    }
                }

                PendingOperation.UPDATE -> {
                    try {
                        val response = issueApi.updateIssue(issue.id, IssueMapper.toDto(issue))
                        if (response.isSuccessful) {
                            issueDao.updateSyncState(issue.id, SyncState.SYNCED, PendingOperation.NONE)
                            syncedCount++
                        } else if (response.code() in 500..599) {
                            return@withContext SyncResult.ServerError(response.code(), "Server temporary error")
                        } else {
                            issueDao.updateSyncState(issue.id, SyncState.FAILED, issue.operationType)
                        }
                    } catch (e: IOException) {
                        return@withContext SyncResult.NetworkError("Network unavailable during update sync", e)
                    } catch (e: Exception) {
                        return@withContext SyncResult.UnexpectedError("Unexpected error during update sync", e)
                    }
                }

                PendingOperation.DELETE -> {
                    try {
                        val response = issueApi.deleteIssue(issue.id)
                        if (response.isSuccessful || response.code() == 404) {
                            // Server confirmed deletion (or record already gone); purge local tombstone
                            issueDao.purgeTombstone(issue.id)
                            syncedCount++
                        } else if (response.code() in 500..599) {
                            return@withContext SyncResult.ServerError(response.code(), "Server temporary error")
                        } else {
                            issueDao.updateSyncState(issue.id, SyncState.FAILED, issue.operationType)
                        }
                    } catch (e: IOException) {
                        return@withContext SyncResult.NetworkError("Network unavailable during delete sync", e)
                    } catch (e: Exception) {
                        return@withContext SyncResult.UnexpectedError("Unexpected error during delete sync", e)
                    }
                }

                PendingOperation.NONE -> {
                    // No pending action needed
                }
            }
        }

        SyncResult.Success(syncedCount)
    }

    /**
     * Pulls remote issues from the REST API and merges them into Room.
     * Applies conflict handling: pending local changes are never overwritten.
     */
    suspend fun pullRemoteIssues(): SyncResult<Unit> = withContext(ioDispatcher) {
        try {
            val response = issueApi.getIssues()
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                val entities = dtos.map { IssueMapper.toEntity(it) }
                issueDao.mergeRemoteIssues(entities)
                SyncResult.Success(Unit)
            } else {
                SyncResult.ServerError(response.code(), response.message())
            }
        } catch (e: IOException) {
            SyncResult.NetworkError("Network unavailable while pulling remote issues", e)
        } catch (e: Exception) {
            SyncResult.UnexpectedError("Unexpected error during remote fetch", e)
        }
    }

    /**
     * Enqueues a OneTimeWorkRequest in WorkManager with network constraints and exponential backoff.
     */
    fun enqueueSyncWork() {
        context?.let { ctx ->
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<IssueSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(ctx).enqueueUniqueWork(
                IssueSyncWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
