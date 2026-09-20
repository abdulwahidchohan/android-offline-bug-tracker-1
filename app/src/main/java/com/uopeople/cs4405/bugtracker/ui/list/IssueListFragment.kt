package com.uopeople.cs4405.bugtracker.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.uopeople.cs4405.bugtracker.BugTrackerApplication
import com.uopeople.cs4405.bugtracker.R
import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.databinding.FragmentIssueListBinding
import com.uopeople.cs4405.bugtracker.ui.editor.IssueEditorFragment
import kotlinx.coroutines.launch

/**
 * Fragment displaying the list of active bug issues.
 * Provides pull-to-refresh sync, issue creation navigation, edit navigation,
 * and confirmation dialogs for deletions.
 */
class IssueListFragment : Fragment() {

    private var _binding: FragmentIssueListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IssueListViewModel by viewModels {
        val app = requireActivity().application as BugTrackerApplication
        IssueListViewModel.provideFactory(app.appContainer.issueRepository)
    }

    private lateinit var adapter: IssueAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIssueListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = IssueAdapter(
            onEditClick = { issue -> navigateToEditor(issue.id) },
            onDeleteClick = { issue -> showDeleteConfirmationDialog(issue) }
        )
        binding.rvIssues.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIssues.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.triggerSync()
        }

        binding.fabAddIssue.setOnClickListener {
            navigateToEditor(null)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.issues.collect { list ->
                        adapter.submitList(list)
                        if (list.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.rvIssues.visibility = View.GONE
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvIssues.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        binding.swipeRefresh.isRefreshing = refreshing
                    }
                }

                launch {
                    viewModel.userMessage.collect { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(issue: IssueEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteIssue(issue.id)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun navigateToEditor(issueId: String?) {
        val fragment = IssueEditorFragment.newInstance(issueId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = IssueListFragment()
    }
}
