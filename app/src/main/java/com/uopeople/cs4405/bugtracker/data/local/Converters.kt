package com.uopeople.cs4405.bugtracker.data.local

import androidx.room.TypeConverter

/**
 * Room type converters for domain enums.
 * Serializes enums into standard strings and safely parses strings back with fallbacks.
 */
class Converters {

    @TypeConverter
    fun fromPriority(priority: Priority?): String {
        return priority?.name ?: Priority.MEDIUM.name
    }

    @TypeConverter
    fun toPriority(value: String?): Priority {
        return if (value != null) {
            try {
                Priority.valueOf(value)
            } catch (e: IllegalArgumentException) {
                Priority.MEDIUM
            }
        } else {
            Priority.MEDIUM
        }
    }

    @TypeConverter
    fun fromIssueStatus(status: IssueStatus?): String {
        return status?.name ?: IssueStatus.OPEN.name
    }

    @TypeConverter
    fun toIssueStatus(value: String?): IssueStatus {
        return if (value != null) {
            try {
                IssueStatus.valueOf(value)
            } catch (e: IllegalArgumentException) {
                IssueStatus.OPEN
            }
        } else {
            IssueStatus.OPEN
        }
    }

    @TypeConverter
    fun fromSyncState(syncState: SyncState?): String {
        return syncState?.name ?: SyncState.PENDING.name
    }

    @TypeConverter
    fun toSyncState(value: String?): SyncState {
        return if (value != null) {
            try {
                SyncState.valueOf(value)
            } catch (e: IllegalArgumentException) {
                SyncState.PENDING
            }
        } else {
            SyncState.PENDING
        }
    }

    @TypeConverter
    fun fromPendingOperation(operation: PendingOperation?): String {
        return operation?.name ?: PendingOperation.NONE.name
    }

    @TypeConverter
    fun toPendingOperation(value: String?): PendingOperation {
        return if (value != null) {
            try {
                PendingOperation.valueOf(value)
            } catch (e: IllegalArgumentException) {
                PendingOperation.NONE
            }
        } else {
            PendingOperation.NONE
        }
    }
}
