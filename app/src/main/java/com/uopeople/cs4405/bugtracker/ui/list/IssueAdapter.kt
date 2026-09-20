package com.uopeople.cs4405.bugtracker.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uopeople.cs4405.bugtracker.R
import com.uopeople.cs4405.bugtracker.data.local.IssueEntity
import com.uopeople.cs4405.bugtracker.data.local.Priority
import com.uopeople.cs4405.bugtracker.data.local.SyncState
import com.uopeople.cs4405.bugtracker.databinding.ItemIssueBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for issues list using DiffUtil for efficient UI updates.
 */
class IssueAdapter(
    private val onEditClick: (IssueEntity) -> Unit,
    private val onDeleteClick: (IssueEntity) -> Unit
) : ListAdapter<IssueEntity, IssueAdapter.IssueViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val binding = ItemIssueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IssueViewHolder(
        private val binding: ItemIssueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(issue: IssueEntity) {
            val context = binding.root.context

            binding.tvIssueTitle.text = issue.title

            if (issue.description.isBlank()) {
                binding.tvIssueDescription.visibility = View.GONE
            } else {
                binding.tvIssueDescription.visibility = View.VISIBLE
                binding.tvIssueDescription.text = issue.description
            }

            // Priority styling
            binding.tvPriorityBadge.text = issue.priority.name
            when (issue.priority) {
                Priority.LOW -> {
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_low_text))
                }
                Priority.MEDIUM -> {
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_medium_text))
                }
                Priority.HIGH -> {
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_high_text))
                }
                Priority.CRITICAL -> {
                    binding.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_critical_text))
                }
            }

            // Status styling
            binding.tvStatusBadge.text = issue.status.name.replace('_', ' ')

            // Textual Sync State (accessible, never communicated through color alone)
            when (issue.syncState) {
                SyncState.PENDING -> {
                    binding.tvSyncBadge.text = context.getString(R.string.sync_state_pending)
                    binding.tvSyncBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                    binding.tvSyncBadge.setTextColor(ContextCompat.getColor(context, R.color.sync_pending_text))
                }
                SyncState.SYNCED -> {
                    binding.tvSyncBadge.text = context.getString(R.string.sync_state_synced)
                    binding.tvSyncBadge.setBackgroundResource(R.drawable.bg_badge_synced)
                    binding.tvSyncBadge.setTextColor(ContextCompat.getColor(context, R.color.sync_synced_text))
                }
                SyncState.FAILED -> {
                    binding.tvSyncBadge.text = context.getString(R.string.sync_state_failed)
                    binding.tvSyncBadge.setBackgroundResource(R.drawable.bg_badge_failed)
                    binding.tvSyncBadge.setTextColor(ContextCompat.getColor(context, R.color.sync_failed_text))
                }
            }

            // Timestamp formatting
            val dateString = dateFormat.format(Date(issue.updatedAt))
            binding.tvTimestamp.text = context.getString(R.string.updated_prefix, dateString)

            // Button actions
            binding.btnEdit.setOnClickListener { onEditClick(issue) }
            binding.btnDelete.setOnClickListener { onDeleteClick(issue) }
            binding.root.setOnClickListener { onEditClick(issue) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<IssueEntity>() {
        override fun areItemsTheSame(oldItem: IssueEntity, newItem: IssueEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IssueEntity, newItem: IssueEntity): Boolean {
            return oldItem == newItem
        }
    }
}
