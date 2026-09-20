package com.uopeople.cs4405.bugtracker.di

import android.content.Context
import com.uopeople.cs4405.bugtracker.data.local.BugTrackerDatabase
import com.uopeople.cs4405.bugtracker.data.remote.IssueApi
import com.uopeople.cs4405.bugtracker.data.remote.RetrofitClient
import com.uopeople.cs4405.bugtracker.data.repository.IssueRepository

/**
 * Dependency container providing singletons for Room database, Retrofit API, and Repository.
 * Follows the standard Android AppContainer pattern for simple, robust dependency injection.
 */
class AppContainer(private val context: Context) {

    val database: BugTrackerDatabase by lazy {
        BugTrackerDatabase.getInstance(context)
    }

    var issueApi: IssueApi = RetrofitClient.issueApi

    val issueRepository: IssueRepository by lazy {
        IssueRepository(
            issueDao = database.issueDao(),
            issueApi = issueApi,
            context = context
        )
    }
}
