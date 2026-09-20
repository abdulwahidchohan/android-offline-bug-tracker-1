package com.uopeople.cs4405.bugtracker.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uopeople.cs4405.bugtracker.BugTrackerApplication
import com.uopeople.cs4405.bugtracker.data.repository.IssueRepository
import com.uopeople.cs4405.bugtracker.data.repository.SyncResult

/**
 * CoroutineWorker responsible for background synchronization of bug tracker issues.
 * Executed under network constraints with exponential backoff and bounded retries.
 */
class IssueSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "com.uopeople.cs4405.bugtracker.sync_worker"
        const val TAG = "IssueSyncWorker"
        const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting synchronization run. Attempt: $runAttemptCount")

        val repository: IssueRepository = try {
            (applicationContext as BugTrackerApplication).appContainer.issueRepository
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve repository from Application container", e)
            return Result.failure()
        }

        // 1. Push pending local mutations to the remote REST API
        when (val pushResult = repository.syncPendingOperations()) {
            is SyncResult.Success -> {
                Log.d(TAG, "Successfully synced ${pushResult.data} pending local mutations.")
            }
            is SyncResult.NetworkError -> {
                Log.w(TAG, "Network error during push sync: ${pushResult.message}. Eligible for retry.")
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
            is SyncResult.ServerError -> {
                Log.w(TAG, "Server error (${pushResult.code}): ${pushResult.message}. Eligible for retry.")
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
            is SyncResult.ValidationError,
            is SyncResult.AuthenticationError -> {
                Log.e(TAG, "Non-retryable error during push sync.")
                return Result.failure()
            }
            is SyncResult.UnexpectedError -> {
                Log.e(TAG, "Unexpected error during push sync: ${pushResult.message}")
                return Result.failure()
            }
        }

        // 2. Pull latest remote issues and merge into Room local database
        when (val pullResult = repository.pullRemoteIssues()) {
            is SyncResult.Success -> {
                Log.d(TAG, "Successfully pulled remote changes into Room database.")
            }
            is SyncResult.NetworkError -> {
                Log.w(TAG, "Network error during pull: ${pullResult.message}")
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
            is SyncResult.ServerError -> {
                Log.w(TAG, "Server error during pull (${pullResult.code})")
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
            is SyncResult.ValidationError,
            is SyncResult.AuthenticationError,
            is SyncResult.UnexpectedError -> {
                Log.e(TAG, "Non-retryable error during pull.")
                return Result.failure()
            }
        }

        return Result.success()
    }
}
