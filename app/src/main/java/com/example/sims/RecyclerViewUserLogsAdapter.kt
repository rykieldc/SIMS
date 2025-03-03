package com.example.sims

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecyclerViewUserLogsAdapter(
    private var logsList: MutableList<LocalHistory>,
    private val context: Context
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

        holder.itemView.setOnClickListener {
            Log.d("RecyclerViewUserLogsAdapter", "Item clicked: ${userLog.name}")
            showDialog(userLog)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDialog(userLog: LocalHistory) {
        val dialogLayout = when {
            userLog.action.startsWith("Added Item") || userLog.action.startsWith("Deleted Item") -> {
                R.layout.dialog_log_add_and_delete
            }
            userLog.action.startsWith("Added User") || userLog.action.startsWith("Deleted User") -> {
                R.layout.dialog_log_user_add_and_delete
            }
            else -> {
                R.layout.dialog_log_update
            }
        }

        val dialogView = LayoutInflater.from(context).inflate(dialogLayout, null)

        val itemImg: ImageView? = dialogView.findViewById(R.id.itemImg)
        val itemName: TextView? = dialogView.findViewById(R.id.itemName)
        val itemUnits: TextView? = dialogView.findViewById(R.id.itemUnits)
        val itemDescription: TextView? = dialogView.findViewById(R.id.logUpdate)
        val itemCode: TextView? = dialogView.findViewById(R.id.itemCode)
        val itemCategory: TextView? = dialogView.findViewById(R.id.itemCategory)
        val itemLocation: TextView? = dialogView.findViewById(R.id.itemLocation)
        val itemSupplier: TextView? = dialogView.findViewById(R.id.itemSupplier)
        val itemDateAdded: TextView? = dialogView.findViewById(R.id.itemDateAdded)
        val itemLastRestocked: TextView? = dialogView.findViewById(R.id.itemLastRestocked)
        val userName: TextView? = dialogView.findViewById(R.id.userName)
        val userUsername: TextView? = dialogView.findViewById(R.id.userUsername)
        val userRole: TextView? = dialogView.findViewById(R.id.userRole)

        itemName?.text = userLog.itemName ?: "N/A"
        itemUnits?.text = "${userLog.stocksLeft ?: 0} unit(s)"
        itemDescription?.text = userLog.itemDetails ?: "No details available"
        itemCode?.text = userLog.itemCode ?: "N/A"
        itemCategory?.text = userLog.itemCategory ?: "N/A"
        itemLocation?.text = userLog.location ?: "N/A"
        itemSupplier?.text = userLog.supplier ?: "N/A"
        itemDateAdded?.text = userLog.dateAdded ?: "N/A"
        itemLastRestocked?.text = userLog.lastRestocked ?: "N/A"
        userName?.text = userLog.userName ?: "N/A"
        userUsername?.text = userLog.userUsername ?: "N/A"
        userRole?.text = userLog.userRole ?: "N/A"

        if (userLog.imageUrl != null) {
            itemImg?.let { imageView ->
                Glide.with(context).load(userLog.imageUrl).into(imageView)
            }
        } else {
            itemImg?.setImageResource(R.drawable.ic_img_placeholder)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle(userLog.action)
            .setPositiveButton("OK") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()
        dialog.show()
    }

    class LogsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val historyDate: TextView = itemView.findViewById(R.id.history_date)
        val historyName: TextView = itemView.findViewById(R.id.history_name)
        val historyAction: TextView = itemView.findViewById(R.id.history_action)
    }

    fun filter(query: String) {
        val filteredList = originalList.filter { log ->
            log.name.lowercase().contains(query.lowercase()) ||
                    log.action.lowercase().contains(query.lowercase())
        }
        updateLogsList(filteredList)
    }

    fun resetList() {
        updateLogsList(originalList)
    }

    private fun updateLogsList(newList: List<LocalHistory>) {
        val diffCallback = LogDiffCallback(logsList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        logsList.clear()
        logsList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }


    class LogDiffCallback(
        private val oldList: List<LocalHistory>,
        private val newList: List<LocalHistory>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].name == newList[newItemPosition].name
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

}
