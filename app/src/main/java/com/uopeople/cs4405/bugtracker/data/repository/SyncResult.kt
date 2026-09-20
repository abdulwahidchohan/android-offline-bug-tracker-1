package com.uopeople.cs4405.bugtracker.data.repository

/**
 * Explicit generic result hierarchy for synchronization and data operations.
 * Allows UI and workers to handle distinct failure classes without leaking stack traces.
 */
sealed interface SyncResult<out T> {
    data class Success<out T>(val data: T) : SyncResult<T>
    data class NetworkError(val message: String, val cause: Throwable? = null) : SyncResult<Nothing>
    data class ServerError(val code: Int, val message: String) : SyncResult<Nothing>
    data class ValidationError(val message: String) : SyncResult<Nothing>
    data class AuthenticationError(val message: String) : SyncResult<Nothing>
    data class UnexpectedError(val message: String, val cause: Throwable? = null) : SyncResult<Nothing>
}
