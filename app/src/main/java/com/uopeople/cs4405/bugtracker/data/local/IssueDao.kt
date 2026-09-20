package com.uopeople.cs4405.bugtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for IssueEntity in Room.
 * Defines all SQL queries, reactive Flow streams, and conflict-safe transactions.
 */
@Dao
interface IssueDao {

    /**
     * Observes all active (non-deleted) issues, sorted by latest update timestamp.
     */
    @Query("SELECT * FROM issues WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeActiveIssues(): Flow<List<IssueEntity>>

    /**
     * Retrieves a single issue by its UUID primary key.
     */
    @Query("SELECT * FROM issues WHERE id = :id LIMIT 1")
    suspend fun getIssueById(id: String): IssueEntity?

    /**
     * Inserts an issue or replaces it on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: IssueEntity)

    /**
     * Inserts a list of issues.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssues(issues: List<IssueEntity>)

    /**
     * Updates an existing issue record.
     */
    @Update
    suspend fun updateIssue(issue: IssueEntity)

    /**
     * Marks an issue logically deleted (tombstone pattern) and queues a remote DELETE operation.
     */
    @Query("UPDATE issues SET isDeleted = 1, syncState = 'PENDING', operationType = 'DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markLogicalDelete(id: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Retrieves all issues that have pending or failed sync operations queued for WorkManager.
     */
    @Query("SELECT * FROM issues WHERE syncState = 'PENDING' OR syncState = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<IssueEntity>

    /**
     * Updates the synchronization state and pending operation for a given issue.
     */
    @Query("UPDATE issues SET syncState = :syncState, operationType = :operationType WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: SyncState, operationType: PendingOperation)

    /**
     * Permanently purges a tombstone record only after successful server deletion confirmation.
     * Architectural guard: ensures pending deletion intents are never prematurely purged.
     */
    @Query("DELETE FROM issues WHERE id = :id AND isDeleted = 1 AND syncState = 'SYNCED'")
    suspend fun purgeTombstone(id: String)

    /**
     * Permanently purges an issue by ID (used when an issue created offline is deleted before ever reaching the server).
     */
    @Query("DELETE FROM issues WHERE id = :id")
    suspend fun deleteIssuePermanently(id: String)

    /**
     * Conflict-Safe Merge Transaction:
     * Merges remote issues into Room while ensuring uncommitted local changes (PENDING)
     * are NEVER silently overwritten by remote state.
     */
    @Transaction
    suspend fun mergeRemoteIssues(remoteIssues: List<IssueEntity>) {
        for (remote in remoteIssues) {
            val local = getIssueById(remote.id)
            if (local == null) {
                // Not present locally; safe to insert if not marked deleted
                if (!remote.isDeleted) {
                    insertIssue(remote.copy(syncState = SyncState.SYNCED, operationType = PendingOperation.NONE))
                }
            } else {
                // If local row is PENDING, local mutation takes precedence! Do not overwrite.
                if (local.syncState == SyncState.PENDING) {
                    continue
                }
                // If local row is SYNCED, newest update wins
                if (remote.updatedAt >= local.updatedAt) {
                    if (remote.isDeleted) {
                        deleteIssuePermanently(remote.id)
                    } else {
                        insertIssue(remote.copy(syncState = SyncState.SYNCED, operationType = PendingOperation.NONE))
                    }
                }
            }
        }
    }
}
