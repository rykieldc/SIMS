package com.example.sims

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewProductNotificationAdapter(
    private var notifications: List<NotificationItem>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_NOTIFICATION = 1
    }

    private val originalList = notifications.toMutableList()

    override fun getItemViewType(position: Int): Int {
        return when (notifications[position]) {
            is NotificationItem.DateHeader -> TYPE_DATE_HEADER
            is NotificationItem.NotificationEntry -> TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_date_header, parent, false)
                DateViewHolder(view)
            }
            TYPE_NOTIFICATION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_notification_item, parent, false)
                NotificationViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = notifications[position]) {
            is NotificationItem.DateHeader -> (holder as DateViewHolder).bind(item)
            is NotificationItem.NotificationEntry -> (holder as NotificationViewHolder).bind(item.notification, onNotificationClick)
        }
    }

    override fun getItemCount() = notifications.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.dateHeaderText)

        fun bind(dateHeader: NotificationItem.DateHeader) {
            dateText.text = dateHeader.date
        }
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val notificationIcon: ImageView = itemView.findViewById(R.id.notificationIcon)
        private val notificationText: TextView = itemView.findViewById(R.id.notificationText)

        fun bind(notification: Notification, onClick: (Notification) -> Unit) {
            notificationText.text = notification.details

            when (notification.icon) {
                "low" -> {
                    notificationIcon.setImageResource(R.drawable.ic_important)
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.low_stock_unread)
                    )
                }
                "critical" -> {
                    notificationIcon.setImageResource(R.drawable.ic_high_priority)
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.critical_stock_unread)
                    )
                }
            }

            itemView.setOnClickListener {
                onClick(notification)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        if (query.isEmpty()) {
            notifications = originalList
        } else {
            val groupedFilteredList = mutableListOf<NotificationItem>()
            val groupedMap = originalList
                .filterIsInstance<NotificationItem.NotificationEntry>()
                .groupBy { it.notification.date }

            for ((date, notifs) in groupedMap) {
                val filteredNotifs = notifs.filter {
                    it.notification.details.contains(query, ignoreCase = true) ||
                            it.notification.itemCode.contains(query, ignoreCase = true)
                }

                if (filteredNotifs.isNotEmpty()) {
                    groupedFilteredList.add(NotificationItem.DateHeader(date))
                    groupedFilteredList.addAll(filteredNotifs)
                }
            }
            notifications = groupedFilteredList
        }
        notifyDataSetChanged()
    }

}

