package com.uopeople.cs4405.bugtracker.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit REST API interface for remote Issue CRUD operations.
 */
interface IssueApi {

    /**
     * Retrieves all issues from the remote server.
     */
    @GET("issues")
    suspend fun getIssues(): Response<List<IssueDto>>

    /**
     * Creates a new issue on the remote server.
     */
    @POST("issues")
    suspend fun createIssue(@Body issue: IssueDto): Response<IssueDto>

    /**
     * Updates an existing issue on the remote server by ID.
     */
    @PUT("issues/{id}")
    suspend fun updateIssue(
        @Path("id") id: String,
        @Body issue: IssueDto
    ): Response<IssueDto>

    /**
     * Deletes an issue on the remote server by ID.
     */
    @DELETE("issues/{id}")
    suspend fun deleteIssue(@Path("id") id: String): Response<Unit>
}
