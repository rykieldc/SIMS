package com.example.sims

import SessionManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.absoluteValue

class StatisticsDashboard : Fragment() {

    private val historyRef = FirebaseDatabase.getInstance().getReference("history")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TextViews for showing counts
        val noOfItemsTextView = view.findViewById<TextView>(R.id.noOfItems)
        val noOfAlertsTextView = view.findViewById<TextView>(R.id.noOfAlerts)
        val topSellingItemTextView = view.findViewById<TextView>(R.id.topSellingItem)

        // Fetch enabled items and update the "noOfItems" TextView
        fetchItems { enabledItems ->
            val enabledItemsCount = enabledItems.size
            noOfItemsTextView.text = "$enabledItemsCount"
        }

        // Fetch enabled notifications and update the "noOfAlerts" TextView
        fetchNotifications { enabledNotifications ->
            val enabledNotificationsCount = enabledNotifications.size
            noOfAlertsTextView.text = "$enabledNotificationsCount"
        }

        // Fetch history data for the current month and year, and calculate the top-selling item
        val calendar = Calendar.getInstance()
        val selectedMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val selectedYear = calendar.get(Calendar.YEAR).toString()

        Log.d("STATISTICS_DEBUG", "Fetching history for Month: $selectedMonth, Year: $selectedYear")

        fetchMonthlyHistoryData { itemSalesCount ->
            val topSellingItem = itemSalesCount.maxByOrNull { it.value } // Find the item with the highest sales
            if (topSellingItem != null) {
                val (itemName, salesCount) = topSellingItem
                Log.d("STATISTICS_DEBUG", "Top Selling Item: $itemName with $salesCount sales.")
                topSellingItemTextView.text = "$itemName \n ($salesCount sold)"
            } else {
                Log.d("STATISTICS_DEBUG", "No sales data found.")
                topSellingItemTextView.text = "N/A"
            }
        }
    }

    // Fetch enabled items
    fun fetchItems(callback: (List<Item>) -> Unit) {
        val itemsRef = FirebaseDatabase.getInstance().getReference("items") // Your reference path
        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemsList = mutableListOf<Item>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(Item::class.java)
                    if (item != null && item.enabled) {
                        itemsList.add(item)
                    } else {
                        Log.e("FetchItems", "Item is null or not enabled: $itemSnapshot")
                    }
                }

                val reversedItemsList = itemsList.asReversed()
                callback(reversedItemsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchItems", "Failed to fetch items: ${error.message}")
                callback(emptyList())
            }
        })
    }

    // Fetch enabled notifications
    fun fetchNotifications(callback: (List<Notification>) -> Unit) {
        val notificationsRef = FirebaseDatabase.getInstance().getReference("notifications")
        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificationsList = mutableListOf<Notification>()
                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(Notification::class.java)
                    if (notification != null && notification.enabled) {
                        notificationsList.add(notification)
                    } else {
                        Log.e("FetchNotifications", "Notification is null or not enabled: $notificationSnapshot")
                    }
                }

                callback(notificationsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FetchNotifications", "Failed to fetch notifications: ${error.message}")
                callback(emptyList())
            }
        })
    }

    // Fetch history data for the given month and year
    private fun fetchMonthlyHistoryData(callback: (Map<String, Int>) -> Unit) {
        val calendar = Calendar.getInstance()
        val currentMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1)  // Ensure two-digit format
        val currentYear = calendar.get(Calendar.YEAR).toString()  // Full year format

        Log.d("STATISTICS_DEBUG", "Fetching history for Month: $currentMonth, Year: $currentYear")

        historyRef.get().addOnSuccessListener { snapshot ->
            val itemSalesCount = mutableMapOf<String, Int>()

            for (doc in snapshot.children) {
                val history = doc.getValue(History::class.java) ?: continue

                // Ensure date is in MM/dd/yy format before splitting
                val parts = history.date.split("/")
                if (parts.size != 3) {
                    Log.w("STATISTICS_DEBUG", "Skipping invalid date format: ${history.date}")
                    continue
                }

                val month = parts[0]  // MM
                val year = "20" + parts[2]  // Convert yy to yyyy

                // Check if record matches current month and year
                if (month == currentMonth && year == currentYear) {
                    Log.d("STATISTICS_DEBUG", "✅ Matched record: ${history.action}")

                    val itemName = extractItemName(history.action)
                    val stockChange = extractStockChange(history.itemDetails)

                    if (itemName != null) {
                        Log.d("STATISTICS_DEBUG", "📌 Extracted Item: $itemName")
                    } else {
                        Log.w("STATISTICS_DEBUG", "⚠️ Failed to extract item name from action: ${history.action}")
                        continue
                    }

                    Log.d("STATISTICS_DEBUG", "🔢 Stock Change: $stockChange")

                    if (history.action.startsWith("Changed")) {
                        val salesCount = itemSalesCount.getOrDefault(itemName, 0) + stockChange.absoluteValue
                        itemSalesCount[itemName] = salesCount

                        Log.d("STATISTICS_DEBUG", "📊 Updated Sales Count: $salesCount for $itemName")
                    }
                }
            }

            // Final check
            if (itemSalesCount.isEmpty()) {
                Log.d("STATISTICS_DEBUG", "❌ No matching sales data found for this month.")
            }

            callback(itemSalesCount)
        }.addOnFailureListener {
            Log.e("STATISTICS_DEBUG", "🚨 Failed to fetch history data: ${it.message}")
            callback(emptyMap())
        }
    }

    // Extract item name from action string
    private fun extractItemName(action: String?): String? {
        if (action == null) return null
        val regex = Regex("\\[(.*?)\\]")
        return regex.find(action)?.groups?.get(1)?.value
    }

    // Extract stock change from item details
    private fun extractStockChange(itemDetails: String?): Int {
        if (itemDetails.isNullOrEmpty()) return 0

        val regex = Regex("Changed from \\[(\\d+)] to \\[(\\d+)]")
        val match = regex.find(itemDetails)

        return if (match != null) {
            val (oldStock, newStock) = match.destructured
            val difference = newStock.toInt() - oldStock.toInt()
            difference
        } else {
            0
        }
    }

}

