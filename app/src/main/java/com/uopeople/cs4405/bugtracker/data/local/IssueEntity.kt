package com.uopeople.cs4405.bugtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Priority levels for bug tracker issues.
 */
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Workflow status lifecycle for bug tracker issues.
 */
enum class IssueStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

/**
 * Synchronization state reflecting local Room vs remote server status.
 */
enum class SyncState {
    PENDING,
    SYNCED,
    FAILED
}

/**
 * Specific pending CRUD operation queued for remote synchronization.
 */
enum class PendingOperation {
    CREATE,
    UPDATE,
    DELETE,
    NONE
}

/**
 * Room database entity representing an issue ticket.
 * Serves as the single local source of truth for the application.
 */
@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val status: IssueStatus = IssueStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING,
    val isDeleted: Boolean = false,
    val operationType: PendingOperation = PendingOperation.CREATE,
    val serverVersion: Long = 1L
)
