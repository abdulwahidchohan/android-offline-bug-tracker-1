package com.uopeople.cs4405.bugtracker.data.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/**
 * Interface-compatible in-memory fake implementation of IssueApi.
 * Used exclusively for local unit tests and demonstrations without requiring a live remote backend.
 */
class FakeIssueApi : IssueApi {

    val issuesMap = mutableMapOf<String, IssueDto>()
    var shouldSimulateNetworkError: Boolean = false
    var shouldSimulateServerError: Boolean = false

    override suspend fun getIssues(): Response<List<IssueDto>> {
        checkErrors()
        return Response.success(issuesMap.values.toList())
    }

    override suspend fun createIssue(issue: IssueDto): Response<IssueDto> {
        checkErrors()
        issuesMap[issue.id] = issue
        return Response.success(issue)
    }

    override suspend fun updateIssue(id: String, issue: IssueDto): Response<IssueDto> {
        checkErrors()
        issuesMap[id] = issue
        return Response.success(issue)
    }

    override suspend fun deleteIssue(id: String): Response<Unit> {
        checkErrors()
        issuesMap.remove(id)
        return Response.success(Unit)
    }

    private fun checkErrors() {
        if (shouldSimulateNetworkError) {
            throw IOException("Simulated network timeout/connectivity loss")
        }
        if (shouldSimulateServerError) {
            val errorBody = "{\"error\":\"Internal Server Error\"}".toResponseBody("application/json".toMediaTypeOrNull())
            throw retrofit2.HttpException(Response.error<Unit>(500, errorBody))
        }
    }
}
