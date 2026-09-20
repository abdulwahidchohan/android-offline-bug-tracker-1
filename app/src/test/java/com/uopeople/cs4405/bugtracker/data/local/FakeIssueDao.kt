package com.uopeople.cs4405.bugtracker.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory test double for IssueDao.
 * Enables rapid, deterministic repository unit testing without SQLite overhead.
 */
class FakeIssueDao : IssueDao {

    private val issuesMap = mutableMapOf<String, IssueEntity>()
    private val issuesFlow = MutableStateFlow<List<IssueEntity>>(emptyList())

    private fun emit() {
        issuesFlow.value = issuesMap.values.toList()
    }

    override fun observeActiveIssues(): Flow<List<IssueEntity>> {
        return issuesFlow.map { list ->
            list.filter { !it.isDeleted }.sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun getIssueById(id: String): IssueEntity? {
        return issuesMap[id]
    }

    override suspend fun insertIssue(issue: IssueEntity) {
        issuesMap[issue.id] = issue
        emit()
    }

    override suspend fun insertIssues(issues: List<IssueEntity>) {
        issues.forEach { issuesMap[it.id] = it }
        emit()
    }

    override suspend fun updateIssue(issue: IssueEntity) {
        issuesMap[issue.id] = issue
        emit()
    }

    override suspend fun markLogicalDelete(id: String, updatedAt: Long) {
        val existing = issuesMap[id]
        if (existing != null) {
            issuesMap[id] = existing.copy(
                isDeleted = true,
                syncState = SyncState.PENDING,
                operationType = PendingOperation.DELETE,
                updatedAt = updatedAt
            )
            emit()
        }
    }

    override suspend fun getPendingOperations(): List<IssueEntity> {
        return issuesMap.values.filter {
            it.syncState == SyncState.PENDING || it.syncState == SyncState.FAILED
        }.sortedBy { it.createdAt }
    }

    override suspend fun updateSyncState(
        id: String,
        syncState: SyncState,
        operationType: PendingOperation
    ) {
        val existing = issuesMap[id]
        if (existing != null) {
            issuesMap[id] = existing.copy(
                syncState = syncState,
                operationType = operationType
            )
            emit()
        }
    }

    override suspend fun purgeTombstone(id: String) {
        val existing = issuesMap[id]
        if (existing != null && existing.isDeleted && existing.syncState == SyncState.SYNCED) {
            issuesMap.remove(id)
            emit()
        }
    }

    override suspend fun deleteIssuePermanently(id: String) {
        issuesMap.remove(id)
        emit()
    }
}
