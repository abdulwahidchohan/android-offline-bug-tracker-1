package com.uopeople.cs4405.bugtracker

import android.app.Application
import com.uopeople.cs4405.bugtracker.di.AppContainer

/**
 * Application class initializing application-wide dependencies in AppContainer.
 */
class BugTrackerApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
