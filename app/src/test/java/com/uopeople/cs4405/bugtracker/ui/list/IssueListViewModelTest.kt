package com.uopeople.cs4405.bugtracker.ui.list

import com.uopeople.cs4405.bugtracker.data.local.FakeIssueDao
import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.remote.FakeIssueApi
import com.uopeople.cs4405.bugtracker.data.repository.IssueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for IssueListViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IssueListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeIssueDao
    private lateinit var fakeApi: FakeIssueApi
    private lateinit var repository: IssueRepository
    private lateinit var viewModel: IssueListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeIssueDao()
        fakeApi = FakeIssueApi()
        repository = IssueRepository(fakeDao, fakeApi, null, testDispatcher)
        viewModel = IssueListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_emitsEmptyList() = runTest {
        val issues = viewModel.issues.first()
        assertTrue(issues.isEmpty())
    }

    @Test
    fun issuesInDatabase_areObservedByViewModel() = runTest {
        val collectJob = launch { viewModel.issues.collect {} }

        fakeDao.insertIssue(
            IssueEntity(
                id = "id-1",
                title = "Crash on startup",
                description = "Null pointer in splash screen",
                priority = Priority.CRITICAL,
                status = IssueStatus.OPEN
            )
        )
        advanceUntilIdle()

        val list = viewModel.issues.value
        assertEquals(1, list.size)
        assertEquals("Crash on startup", list[0].title)

        collectJob.cancel()
    }

    @Test
    fun deleteIssue_removesIssueFromActiveList() = runTest {
        val collectJob = launch { viewModel.issues.collect {} }

        val issue = IssueEntity(
            id = "del-1",
            title = "Obsolete bug",
            priority = Priority.LOW,
            status = IssueStatus.CLOSED
        )
        fakeDao.insertIssue(issue)
        advanceUntilIdle()

        assertEquals(1, viewModel.issues.value.size)

        viewModel.deleteIssue("del-1")
        advanceUntilIdle()

        // Active issues list in ViewModel should now be empty
        assertEquals(0, viewModel.issues.value.size)

        collectJob.cancel()
    }
}
