package com.uopeople.cs4405.bugtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database for Bug Tracker.
 * Manages local database lifecycle and provides access to IssueDao.
 */
@Database(
    entities = [IssueEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BugTrackerDatabase : RoomDatabase() {

    abstract fun issueDao(): IssueDao

    companion object {
        @Volatile
        private var INSTANCE: BugTrackerDatabase? = null

        fun getInstance(context: Context): BugTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BugTrackerDatabase::class.java,
                    "bug_tracker.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
