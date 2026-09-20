package com.uopeople.cs4405.bugtracker.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.uopeople.cs4405.bugtracker.data.local.FakeIssueDao
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.remote.FakeIssueApi
import com.uopeople.cs4405.bugtracker.data.repository.IssueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for IssueEditorViewModel verifying validation logic
 * and SavedStateHandle state preservation across process recreation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IssueEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeIssueDao
    private lateinit var fakeApi: FakeIssueApi
    private lateinit var repository: IssueRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeIssueDao()
        fakeApi = FakeIssueApi()
        repository = IssueRepository(fakeDao, fakeApi, null, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyTitle_isRejectedWithValidationError() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = IssueEditorViewModel(repository, savedStateHandle)

        viewModel.onTitleChanged("   ")
        viewModel.saveIssue()
        advanceUntilIdle()

        assertEquals("Title cannot be empty", viewModel.validationError.value)
    }

    @Test
    fun validIssue_isSubmittedSuccessfully() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = IssueEditorViewModel(repository, savedStateHandle)

        viewModel.onTitleChanged("Memory Leak in RecyclerView")
        viewModel.onDescriptionChanged("Bitmap allocation not recycled properly")
        viewModel.onPriorityChanged(Priority.HIGH)
        viewModel.onStatusChanged(IssueStatus.OPEN)

        viewModel.saveIssue()
        advanceUntilIdle()

        assertNull(viewModel.validationError.value)

        // Verify it was stored in Room
        val saved = fakeDao.observeActiveIssues().first()
        assertEquals(1, saved.size)
        assertEquals("Memory Leak in RecyclerView", saved[0].title)
        assertEquals(Priority.HIGH, saved[0].priority)
    }

    @Test
    fun draftState_isPreservedThroughSavedStateHandle() = runTest {
        // 1. Initial ViewModel instance simulates user typing in fields
        val stateHandle = SavedStateHandle()
        val initialViewModel = IssueEditorViewModel(repository, stateHandle)

        initialViewModel.onTitleChanged("Draft bug report")
        initialViewModel.onDescriptionChanged("Step 1: Rotate screen...")
        initialViewModel.onPriorityChanged(Priority.CRITICAL)
        initialViewModel.onStatusChanged(IssueStatus.IN_PROGRESS)

        // 2. Recreating ViewModel with the SAME SavedStateHandle (simulating process recreation)
        val recreatedViewModel = IssueEditorViewModel(repository, stateHandle)

        // 3. Verify all draft fields are completely restored
        assertEquals("Draft bug report", recreatedViewModel.title.value)
        assertEquals("Step 1: Rotate screen...", recreatedViewModel.description.value)
        assertEquals(Priority.CRITICAL, recreatedViewModel.priority.value)
        assertEquals(IssueStatus.IN_PROGRESS, recreatedViewModel.status.value)
    }
}
