package com.uopeople.cs4405.bugtracker.data.remote

import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.PendingOperation
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.local.SyncState

/**
 * Bi-directional mappings between local Room IssueEntity and remote IssueDto.
 */
object IssueMapper {

    fun toDto(entity: IssueEntity): IssueDto {
        return IssueDto(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            priority = entity.priority.name,
            status = entity.status.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            serverVersion = entity.serverVersion
        )
    }

    fun toEntity(
        dto: IssueDto,
        syncState: SyncState = SyncState.SYNCED,
        operationType: PendingOperation = PendingOperation.NONE
    ): IssueEntity {
        val priorityEnum = try {
            Priority.valueOf(dto.priority.uppercase())
        } catch (e: Exception) {
            Priority.MEDIUM
        }

        val statusEnum = try {
            IssueStatus.valueOf(dto.status.uppercase())
        } catch (e: Exception) {
            IssueStatus.OPEN
        }

        return IssueEntity(
            id = dto.id,
            title = dto.title,
            description = dto.description ?: "",
            priority = priorityEnum,
            status = statusEnum,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            syncState = syncState,
            isDeleted = dto.isDeleted,
            operationType = operationType,
            serverVersion = dto.serverVersion
        )
    }
}
