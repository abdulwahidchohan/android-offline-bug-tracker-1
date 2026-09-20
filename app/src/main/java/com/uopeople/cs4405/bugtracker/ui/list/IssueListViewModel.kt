package com.uopeople.cs4405.bugtracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.data.repository.IssueRepository
import com.uopeople.cs4405.bugtracker.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the active issues list screen.
 * Observes Room Flow, triggers background/manual sync, and manages issue deletions.
 */
class IssueListViewModel(
    private val repository: IssueRepository
) : ViewModel() {

    val issues: StateFlow<List<IssueEntity>> = repository.observeActiveIssues()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun triggerSync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Enqueue work and perform pull
            repository.enqueueSyncWork()
            when (repository.pullRemoteIssues()) {
                is SyncResult.Success -> {
                    _userMessage.emit("Sync complete.")
                }
                is SyncResult.NetworkError -> {
                    _userMessage.emit("Offline: Working with local data.")
                }
                is SyncResult.ServerError -> {
                    _userMessage.emit("Server error: Changes remain stored locally.")
                }
                is SyncResult.ValidationError,
                is SyncResult.AuthenticationError,
                is SyncResult.UnexpectedError -> {
                    _userMessage.emit("Sync issue: Local data preserved.")
                }
            }
            _isRefreshing.value = false
        }
    }

    fun deleteIssue(id: String) {
        viewModelScope.launch {
            when (val result = repository.deleteIssue(id)) {
                is SyncResult.Success -> {
                    _userMessage.emit("Issue deleted locally.")
                }
                is SyncResult.UnexpectedError -> {
                    _userMessage.emit(result.message)
                }
                else -> {
                    _userMessage.emit("Unable to delete issue.")
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: IssueRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return IssueListViewModel(repository) as T
                }
            }
    }
}
