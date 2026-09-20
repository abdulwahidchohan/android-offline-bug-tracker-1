package com.uopeople.cs4405.bugtracker.data.remote

import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.PendingOperation
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.local.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for IssueMapper translating between Room IssueEntity and Retrofit IssueDto.
 */
class IssueMapperTest {

    @Test
    fun toDto_mapsAllFieldsCorrectly() {
        val entity = IssueEntity(
            id = "test-uuid-123",
            title = "Database Migration Crash",
            description = "App crashes on SQLite migration step 2",
            priority = Priority.CRITICAL,
            status = IssueStatus.IN_PROGRESS,
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L,
            syncState = SyncState.PENDING,
            isDeleted = false,
            operationType = PendingOperation.CREATE,
            serverVersion = 2L
        )

        val dto = IssueMapper.toDto(entity)

        assertEquals("test-uuid-123", dto.id)
        assertEquals("Database Migration Crash", dto.title)
        assertEquals("App crashes on SQLite migration step 2", dto.description)
        assertEquals("CRITICAL", dto.priority)
        assertEquals("IN_PROGRESS", dto.status)
        assertEquals(1700000000000L, dto.createdAt)
        assertEquals(1700000050000L, dto.updatedAt)
        assertFalse(dto.isDeleted)
        assertEquals(2L, dto.serverVersion)
    }

    @Test
    fun toEntity_mapsAllFieldsCorrectly() {
        val dto = IssueDto(
            id = "test-uuid-456",
            title = "UI layout clipped",
            description = "Floating action button overlaps last card",
            priority = "HIGH",
            status = "RESOLVED",
            createdAt = 1700000000000L,
            updatedAt = 1700000020000L,
            isDeleted = true,
            serverVersion = 3L
        )

        val entity = IssueMapper.toEntity(dto, SyncState.SYNCED, PendingOperation.NONE)

        assertEquals("test-uuid-456", entity.id)
        assertEquals("UI layout clipped", entity.title)
        assertEquals("Floating action button overlaps last card", entity.description)
        assertEquals(Priority.HIGH, entity.priority)
        assertEquals(IssueStatus.RESOLVED, entity.status)
        assertEquals(SyncState.SYNCED, entity.syncState)
        assertEquals(PendingOperation.NONE, entity.operationType)
        assertTrue(entity.isDeleted)
        assertEquals(3L, entity.serverVersion)
    }

    @Test
    fun toEntity_fallsBackToDefaultsOnInvalidEnums() {
        val dto = IssueDto(
            id = "test-uuid-789",
            title = "Unknown enum values",
            description = null,
            priority = "SUPER_URGENT_CUSTOM",
            status = "PENDING_REVIEW_CUSTOM",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )

        val entity = IssueMapper.toEntity(dto)

        // Should gracefully fall back to default enums rather than crashing
        assertEquals(Priority.MEDIUM, entity.priority)
        assertEquals(IssueStatus.OPEN, entity.status)
        assertEquals("", entity.description)
    }
}
