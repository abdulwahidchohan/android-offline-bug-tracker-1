package com.uopeople.cs4405.bugtracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.uopeople.cs4405.bugtracker.ui.list.IssueListFragment

/**
 * Main Activity hosting the Bug Tracker fragments.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, IssueListFragment.newInstance())
                .commit()
        }
    }
}
