package com.uopeople.cs4405.bugtracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * AndroidX Instrumentation tests verifying Room database operations
 * on real in-memory SQLite instances.
 */
@RunWith(AndroidJUnit4::class)
class IssueDaoInstrumentationTest {

    private lateinit var db: BugTrackerDatabase
    private lateinit var dao: IssueDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BugTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.issueDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadIssue() = runBlocking {
        val issue = IssueEntity(
            id = "issue-1",
            title = "Crash on orientation change",
            description = "Fragment recreation leaks view binding",
            priority = Priority.HIGH,
            status = IssueStatus.OPEN,
            createdAt = 1000L,
            updatedAt = 1000L,
            syncState = SyncState.PENDING,
            operationType = PendingOperation.CREATE
        )

        dao.insertIssue(issue)

        val retrieved = dao.getIssueById("issue-1")
        assertNotNull(retrieved)
        assertEquals("Crash on orientation change", retrieved?.title)
        assertEquals(Priority.HIGH, retrieved?.priority)
    }

    @Test
    fun updateIssue() = runBlocking {
        val issue = IssueEntity(
            id = "issue-2",
            title = "Initial Title",
            priority = Priority.LOW,
            status = IssueStatus.OPEN
        )
        dao.insertIssue(issue)

        val updated = issue.copy(
            title = "Updated Title",
            priority = Priority.CRITICAL,
            status = IssueStatus.RESOLVED
        )
        dao.updateIssue(updated)

        val retrieved = dao.getIssueById("issue-2")
        assertEquals("Updated Title", retrieved?.title)
        assertEquals(Priority.CRITICAL, retrieved?.priority)
        assertEquals(IssueStatus.RESOLVED, retrieved?.status)
    }

    @Test
    fun logicalDelete_hidesFromActiveIssues() = runBlocking {
        val issue = IssueEntity(
            id = "issue-3",
            title = "Active Issue",
            isDeleted = false
        )
        dao.insertIssue(issue)

        val activeBefore = dao.observeActiveIssues().first()
        assertEquals(1, activeBefore.size)

        dao.markLogicalDelete("issue-3", System.currentTimeMillis())

        val activeAfter = dao.observeActiveIssues().first()
        assertEquals(0, activeAfter.size)

        // Raw query should still see the tombstone
        val tombstone = dao.getIssueById("issue-3")
        assertNotNull(tombstone)
        assertTrue(tombstone?.isDeleted == true)
        assertEquals(PendingOperation.DELETE, tombstone?.operationType)
    }

    @Test
    fun pendingOperationsQuery_retrievesPendingAndFailed() = runBlocking {
        val pendingIssue = IssueEntity(id = "p-1", title = "P1", syncState = SyncState.PENDING)
        val failedIssue = IssueEntity(id = "p-2", title = "P2", syncState = SyncState.FAILED)
        val syncedIssue = IssueEntity(id = "p-3", title = "P3", syncState = SyncState.SYNCED)

        dao.insertIssues(listOf(pendingIssue, failedIssue, syncedIssue))

        val pendingList = dao.getPendingOperations()
        assertEquals(2, pendingList.size)
        assertTrue(pendingList.any { it.id == "p-1" })
        assertTrue(pendingList.any { it.id == "p-2" })
    }

    @Test
    fun purgeTombstone_removesLogicallyDeletedIssue() = runBlocking {
        val issue = IssueEntity(id = "tomb-1", title = "Tombstone", isDeleted = true)
        dao.insertIssue(issue)

        dao.purgeTombstone("tomb-1")

        val retrieved = dao.getIssueById("tomb-1")
        assertNull(retrieved)
    }

    @Test
    fun conflictSafeMerge_preservesPendingLocalChanges() = runBlocking {
        val localUnsynced = IssueEntity(
            id = "conflict-1",
            title = "Local Draft Title",
            syncState = SyncState.PENDING,
            updatedAt = 5000L
        )
        dao.insertIssue(localUnsynced)

        val remoteVersion = IssueEntity(
            id = "conflict-1",
            title = "Remote Older Title",
            syncState = SyncState.SYNCED,
            updatedAt = 4000L
        )

        dao.mergeRemoteIssues(listOf(remoteVersion))

        val result = dao.getIssueById("conflict-1")
        assertEquals("Local Draft Title", result?.title)
        assertEquals(SyncState.PENDING, result?.syncState)
    }
}
