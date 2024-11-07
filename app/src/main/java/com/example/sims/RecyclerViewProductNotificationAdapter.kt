package com.example.sims

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewProductNotificationAdapter(private val notifications: List<ProductNotifications>) :
    RecyclerView.Adapter<RecyclerViewProductNotificationAdapter.NotificationViewHolder>() {

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
        holder.notificationText.text = currentItem.text

        when (currentItem.stockLevel) {
            "low" -> {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, if (currentItem.isRead) R.color.low_stock_read else R.color.low_stock_unread))
                holder.notificationIcon.setImageResource(R.drawable.ic_important)
            }
            "critical" -> {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, if (currentItem.isRead) R.color.critical_stock_read else R.color.critical_stock_unread))
                holder.notificationIcon.setImageResource(R.drawable.ic_high_priority)
            }
            else -> {
                // Default background and icon
            }
        }
    }

    override fun getItemCount() = notifications.size
}
