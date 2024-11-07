package com.example.sims

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewProductNotificationAdapter(private var notifications: List<ProductNotifications>) :
    RecyclerView.Adapter<RecyclerViewProductNotificationAdapter.NotificationViewHolder>() {

    private val originalList = notifications.toMutableList()

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationIcon: ImageView = itemView.findViewById(R.id.notificationIcon)
        val notificationText: TextView = itemView.findViewById(R.id.notificationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_notification_item, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentItem = notifications[position]
        holder.notificationText.text = currentItem.details

        when (currentItem.icon) {
            "low" -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_important)
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.low_stock_unread)
                )
            }
            "critical" -> {
                holder.notificationIcon.setImageResource(R.drawable.ic_high_priority)
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.critical_stock_unread)
                )
            }
        }
    }

    override fun getItemCount() = notifications.size

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                it.details.contains(query, ignoreCase = true) ||
                        it.itemCode.contains(query, ignoreCase = true)
            }
        }
        notifications = filteredList
        notifyDataSetChanged()
    }
}