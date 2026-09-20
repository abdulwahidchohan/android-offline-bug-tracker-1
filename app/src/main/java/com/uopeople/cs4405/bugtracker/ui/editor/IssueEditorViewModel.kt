package com.uopeople.cs4405.bugtracker.ui.editor

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.repository.IssueRepository
import com.uopeople.cs4405.bugtracker.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Issue creation and editing.
 * Relies on SavedStateHandle to guarantee draft survival across screen rotations
 * and Android OS process recreation.
 */
class IssueEditorViewModel(
    private val repository: IssueRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val KEY_ISSUE_ID = "key_issue_id"
        const val KEY_TITLE = "key_title"
        const val KEY_DESCRIPTION = "key_description"
        const val KEY_PRIORITY = "key_priority"
        const val KEY_STATUS = "key_status"
    }

    val issueId: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_ISSUE_ID, null)
    val title: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TITLE, "")
    val description: StateFlow<String> = savedStateHandle.getStateFlow(KEY_DESCRIPTION, "")
    val priority: StateFlow<Priority> = savedStateHandle.getStateFlow(KEY_PRIORITY, Priority.MEDIUM)
    val status: StateFlow<IssueStatus> = savedStateHandle.getStateFlow(KEY_STATUS, IssueStatus.OPEN)

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<String>()
    val saveSuccessEvent: SharedFlow<String> = _saveSuccessEvent.asSharedFlow()

    fun initialize(id: String?) {
        if (id != null && savedStateHandle.get<String?>(KEY_ISSUE_ID) != id) {
            savedStateHandle[KEY_ISSUE_ID] = id
            viewModelScope.launch {
                val issue = repository.getIssue(id)
                if (issue != null) {
                    savedStateHandle[KEY_TITLE] = issue.title
                    savedStateHandle[KEY_DESCRIPTION] = issue.description
                    savedStateHandle[KEY_PRIORITY] = issue.priority
                    savedStateHandle[KEY_STATUS] = issue.status
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        savedStateHandle[KEY_TITLE] = newTitle
        if (_validationError.value != null && newTitle.isNotBlank()) {
            _validationError.value = null
        }
    }

    fun onDescriptionChanged(newDescription: String) {
        savedStateHandle[KEY_DESCRIPTION] = newDescription
    }

    fun onPriorityChanged(newPriority: Priority) {
        savedStateHandle[KEY_PRIORITY] = newPriority
    }

    fun onStatusChanged(newStatus: IssueStatus) {
        savedStateHandle[KEY_STATUS] = newStatus
    }

    fun saveIssue() {
        val currentTitle = title.value.trim()
        if (currentTitle.isEmpty()) {
            _validationError.value = "Title cannot be empty"
            return
        }

        viewModelScope.launch {
            val currentId = issueId.value
            if (currentId == null) {
                // Create new issue
                when (val result = repository.createIssue(
                    title = currentTitle,
                    description = description.value,
                    priority = priority.value,
                    status = status.value
                )) {
                    is SyncResult.Success -> {
                        _saveSuccessEvent.emit("Issue saved offline.")
                    }
                    is SyncResult.ValidationError -> {
                        _validationError.value = result.message
                    }
                    else -> {
                        _validationError.value = "Failed to save issue."
                    }
                }
            } else {
                // Update existing issue
                when (val result = repository.updateIssue(
                    id = currentId,
                    title = currentTitle,
                    description = description.value,
                    priority = priority.value,
                    status = status.value
                )) {
                    is SyncResult.Success -> {
                        _saveSuccessEvent.emit("Issue updated offline.")
                    }
                    is SyncResult.ValidationError -> {
                        _validationError.value = result.message
                    }
                    else -> {
                        _validationError.value = "Failed to update issue."
                    }
                }
            }
        }
    }

    class Factory(
        private val repository: IssueRepository,
        owner: SavedStateRegistryOwner
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return IssueEditorViewModel(repository, handle) as T
        }
    }
}
