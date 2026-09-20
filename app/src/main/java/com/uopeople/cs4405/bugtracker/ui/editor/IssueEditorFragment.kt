package com.uopeople.cs4405.bugtracker.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.uopeople.cs4405.bugtracker.BugTrackerApplication
import com.uopeople.cs4405.bugtracker.R
import com.uopeople.cs4405.bugtracker.data.local.IssueStatus
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.databinding.FragmentIssueEditorBinding
import kotlinx.coroutines.launch

/**
 * Fragment for creating and editing bug issues.
 * Retains editor draft state across configuration changes and process death
 * via SavedStateHandle in IssueEditorViewModel.
 */
class IssueEditorFragment : Fragment() {

    private var _binding: FragmentIssueEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IssueEditorViewModel by viewModels {
        val app = requireActivity().application as BugTrackerApplication
        IssueEditorViewModel.Factory(app.appContainer.issueRepository, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIssueEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val issueId = arguments?.getString(ARG_ISSUE_ID)
        viewModel.initialize(issueId)

        setupToolbar(issueId != null)
        setupInputListeners()
        observeViewModel()
    }

    private fun setupToolbar(isEditing: Boolean) {
        binding.toolbar.title = if (isEditing) {
            getString(R.string.title_edit_issue)
        } else {
            getString(R.string.title_create_issue)
        }
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupInputListeners() {
        binding.etTitle.doAfterTextChanged { text ->
            viewModel.onTitleChanged(text?.toString().orEmpty())
        }

        binding.etDescription.doAfterTextChanged { text ->
            viewModel.onDescriptionChanged(text?.toString().orEmpty())
        }

        binding.rgPriority.setOnCheckedChangeListener { _, checkedId ->
            val priority = when (checkedId) {
                R.id.rbPriorityLow -> Priority.LOW
                R.id.rbPriorityMedium -> Priority.MEDIUM
                R.id.rbPriorityHigh -> Priority.HIGH
                R.id.rbPriorityCritical -> Priority.CRITICAL
                else -> Priority.MEDIUM
            }
            viewModel.onPriorityChanged(priority)
        }

        binding.rgStatus.setOnCheckedChangeListener { _, checkedId ->
            val status = when (checkedId) {
                R.id.rbStatusOpen -> IssueStatus.OPEN
                R.id.rbStatusInProgress -> IssueStatus.IN_PROGRESS
                R.id.rbStatusResolved -> IssueStatus.RESOLVED
                R.id.rbStatusClosed -> IssueStatus.CLOSED
                else -> IssueStatus.OPEN
            }
            viewModel.onStatusChanged(status)
        }

        binding.btnSaveIssue.setOnClickListener {
            viewModel.saveIssue()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.title.collect { currentTitle ->
                        if (binding.etTitle.text?.toString() != currentTitle) {
                            binding.etTitle.setText(currentTitle)
                        }
                    }
                }

                launch {
                    viewModel.description.collect { currentDesc ->
                        if (binding.etDescription.text?.toString() != currentDesc) {
                            binding.etDescription.setText(currentDesc)
                        }
                    }
                }

                launch {
                    viewModel.priority.collect { priority ->
                        val targetRadioId = when (priority) {
                            Priority.LOW -> R.id.rbPriorityLow
                            Priority.MEDIUM -> R.id.rbPriorityMedium
                            Priority.HIGH -> R.id.rbPriorityHigh
                            Priority.CRITICAL -> R.id.rbPriorityCritical
                        }
                        if (binding.rgPriority.checkedRadioButtonId != targetRadioId) {
                            binding.rgPriority.check(targetRadioId)
                        }
                    }
                }

                launch {
                    viewModel.status.collect { status ->
                        val targetRadioId = when (status) {
                            IssueStatus.OPEN -> R.id.rbStatusOpen
                            IssueStatus.IN_PROGRESS -> R.id.rbStatusInProgress
                            IssueStatus.RESOLVED -> R.id.rbStatusResolved
                            IssueStatus.CLOSED -> R.id.rbStatusClosed
                        }
                        if (binding.rgStatus.checkedRadioButtonId != targetRadioId) {
                            binding.rgStatus.check(targetRadioId)
                        }
                    }
                }

                launch {
                    viewModel.validationError.collect { error ->
                        binding.tilTitle.error = error
                    }
                }

                launch {
                    viewModel.saveSuccessEvent.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ISSUE_ID = "arg_issue_id"

        fun newInstance(issueId: String? = null): IssueEditorFragment {
            return IssueEditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ISSUE_ID, issueId)
                }
            }
        }
    }
}
