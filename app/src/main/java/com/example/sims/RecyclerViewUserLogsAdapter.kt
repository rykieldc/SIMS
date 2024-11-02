package com.example.sims

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewUserLogsAdapter(
    private var logsList: MutableList<UserLogs> = mutableListOf()
) : RecyclerView.Adapter<RecyclerViewUserLogsAdapter.LogsViewHolder>() {

    var originalList = logsList.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_user_activity, parent, false)
        return LogsViewHolder(view)
    }

    override fun getItemCount(): Int = logsList.size

    override fun onBindViewHolder(holder: LogsViewHolder, position: Int) {
        val userLog = logsList[position]
        holder.historyDate.text = userLog.date
        holder.historyName.text = userLog.name
        holder.historyAction.text = userLog.action
    }

    class LogsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val historyDate: TextView = itemView.findViewById(R.id.history_date)
        val historyName: TextView = itemView.findViewById(R.id.history_name)
        val historyAction: TextView = itemView.findViewById(R.id.history_action)
    }

    fun filter(query: String) {
        val filteredList = originalList.filter { log ->
            log.name.lowercase().contains(query.lowercase())
        }
        updateLogsList(filteredList)
    }

    fun resetList() {
        updateLogsList(originalList)
    }

    private fun updateLogsList(newList: List<UserLogs>) {
        val diffCallback = LogDiffCallback(logsList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        logsList.clear()
        logsList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    class LogDiffCallback(
        private val oldList: List<UserLogs>,
        private val newList: List<UserLogs>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].name == newList[newItemPosition].name
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}
