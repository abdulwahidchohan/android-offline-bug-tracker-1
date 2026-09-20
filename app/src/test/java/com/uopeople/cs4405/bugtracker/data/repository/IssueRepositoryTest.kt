package com.uopeople.cs4405.bugtracker.data.repository

import com.uopeople.cs4405.bugtracker.data.local.FakeIssueDao
import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.PendingOperation
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.local.SyncState
import com.uopeople.cs4405.bugtracker.data.remote.FakeIssueApi
import com.uopeople.cs4405.bugtracker.data.remote.IssueDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying offline-first repository invariants:
 * - Room as single source of truth
 * - State transitions: PENDING -> SYNCED
 * - Network failures leave PENDING state intact
 * - Tombstones preserved across failed remote deletes and purged upon success
 * - Offline created-then-deleted records purged immediately
 * - Local uncommitted mutations protected from remote overwrite
 */
class IssueRepositoryTest {

    private lateinit var fakeDao: FakeIssueDao
    private lateinit var fakeApi: FakeIssueApi
    private lateinit var repository: IssueRepository

    @Before
    fun setUp() {
        fakeDao = FakeIssueDao()
        fakeApi = FakeIssueApi()
        // Pass null context so WorkManager isn't scheduled inside pure unit tests
        repository = IssueRepository(fakeDao, fakeApi, null)
    }

    @Test
    fun offlineCreate_remainsLocallyAvailableWithPendingState() = runTest {
        val result = repository.createIssue(
            title = "App freezes on rotate",
            description = "Observed when rotating phone in portrait mode",
            priority = Priority.HIGH,
            status = IssueStatus.OPEN
        )

        assertTrue(result is SyncResult.Success)
        val created = (result as SyncResult.Success).data

        // 1. Available in DAO immediately
        val savedInDb = fakeDao.getIssueById(created.id)
        assertNotNull(savedInDb)
        assertEquals("App freezes on rotate", savedInDb?.title)

        // 2. Marked PENDING with operation CREATE
        assertEquals(SyncState.PENDING, savedInDb?.syncState)
        assertEquals(PendingOperation.CREATE, savedInDb?.operationType)

        // 3. Appears in observable active issues
        val activeIssues = repository.observeActiveIssues().first()
        assertEquals(1, activeIssues.size)
        assertEquals(created.id, activeIssues[0].id)
    }

    @Test
    fun syncPendingOperations_successfulCreate_changesStateToSynced() = runTest {
        val createResult = repository.createIssue("Sync Test", "Desc", Priority.LOW, IssueStatus.OPEN)
        val issueId = (createResult as SyncResult.Success).data.id

        val syncResult = repository.syncPendingOperations()
        assertTrue(syncResult is SyncResult.Success)
        assertEquals(1, (syncResult as SyncResult.Success).data)

        val syncedIssue = fakeDao.getIssueById(issueId)
        assertEquals(SyncState.SYNCED, syncedIssue?.syncState)
        assertEquals(PendingOperation.NONE, syncedIssue?.operationType)

        // Verify it was saved on fake remote API
        assertNotNull(fakeApi.issuesMap[issueId])
    }

    @Test
    fun syncPendingOperations_networkFailure_preservesPendingState() = runTest {
        val createResult = repository.createIssue("Offline Test", "Desc", Priority.MEDIUM, IssueStatus.OPEN)
        val issueId = (createResult as SyncResult.Success).data.id

        // Simulate network error
        fakeApi.shouldSimulateNetworkError = true

        val syncResult = repository.syncPendingOperations()
        assertTrue(syncResult is SyncResult.NetworkError)

        // Database row MUST remain PENDING for subsequent WorkManager retry
        val stored = fakeDao.getIssueById(issueId)
        assertEquals(SyncState.PENDING, stored?.syncState)
        assertEquals(PendingOperation.CREATE, stored?.operationType)
    }

    @Test
    fun deleteIssue_successfulDelete_purgesTombstone() = runTest {
        // First create and sync the issue so it exists remotely
        val createResult = repository.createIssue("To be deleted", "", Priority.LOW, IssueStatus.OPEN)
        val issueId = (createResult as SyncResult.Success).data.id
        repository.syncPendingOperations()

        // Delete the issue
        val deleteResult = repository.deleteIssue(issueId)
        assertTrue(deleteResult is SyncResult.Success)

        // It is marked logically deleted (tombstone) and hidden from active list
        val tombstone = fakeDao.getIssueById(issueId)
        assertTrue(tombstone?.isDeleted == true)
        val activeList = repository.observeActiveIssues().first()
        assertTrue(activeList.isEmpty())

        // Sync the deletion remotely
        repository.syncPendingOperations()

        // After successful server response, tombstone is purged from local DB
        assertNull(fakeDao.getIssueById(issueId))
        assertNull(fakeApi.issuesMap[issueId])
    }

    @Test
    fun deleteIssue_failedDelete_preservesTombstone() = runTest {
        val createResult = repository.createIssue("Delete Fail", "", Priority.LOW, IssueStatus.OPEN)
        val issueId = (createResult as SyncResult.Success).data.id
        repository.syncPendingOperations()

        repository.deleteIssue(issueId)

        // Simulate network failure during delete sync
        fakeApi.shouldSimulateNetworkError = true
        val syncResult = repository.syncPendingOperations()
        assertTrue(syncResult is SyncResult.NetworkError)

        // Tombstone MUST still exist in database for future retry
        val preservedTombstone = fakeDao.getIssueById(issueId)
        assertNotNull(preservedTombstone)
        assertTrue(preservedTombstone?.isDeleted == true)
        assertEquals(PendingOperation.DELETE, preservedTombstone?.operationType)
    }

    @Test
    fun deleteIssue_offlineCreatedIssue_purgedImmediatelyWithoutRemoteDelete() = runTest {
        // Create an issue offline (PENDING + CREATE)
        val createResult = repository.createIssue("Created & Deleted Offline", "", Priority.LOW, IssueStatus.OPEN)
        val issueId = (createResult as SyncResult.Success).data.id

        // Delete while still offline and unsynced
        repository.deleteIssue(issueId)

        // Should be purged immediately from local database without needing remote delete
        assertNull(fakeDao.getIssueById(issueId))
    }

    @Test
    fun pullRemoteIssues_olderRemoteData_doesNotOverwriteNewerPendingLocalData() = runTest {
        val localIssue = IssueEntity(
            id = "issue-conflict-1",
            title = "Local Newer Title",
            description = "Local Description",
            priority = Priority.CRITICAL,
            status = IssueStatus.IN_PROGRESS,
            createdAt = 1000L,
            updatedAt = 5000L,
            syncState = SyncState.PENDING,
            isDeleted = false,
            operationType = PendingOperation.UPDATE
        )
        fakeDao.insertIssue(localIssue)

        // Remote has an older record
        fakeApi.issuesMap["issue-conflict-1"] = IssueDto(
            id = "issue-conflict-1",
            title = "Old Remote Title",
            description = "Old Remote Description",
            priority = "LOW",
            status = "OPEN",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        // When pulling remote issues
        val pullResult = repository.pullRemoteIssues()
        assertTrue(pullResult is SyncResult.Success)

        // Local uncommitted change MUST be preserved!
        val preservedLocal = fakeDao.getIssueById("issue-conflict-1")
        assertEquals("Local Newer Title", preservedLocal?.title)
        assertEquals(SyncState.PENDING, preservedLocal?.syncState)
    }
}
