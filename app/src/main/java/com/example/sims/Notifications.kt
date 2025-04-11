package com.example.sims

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Notifications : Fragment() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: RecyclerViewProductNotificationAdapter
    private lateinit var searchView: SearchView
    private val firebaseDatabaseHelper = FirebaseDatabaseHelper()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationRecyclerView = view.findViewById(R.id.rvNotification)
        notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchView = view.findViewById(R.id.searchProduct)
        setupSearchView()

        val notificationDao = App.database.notificationDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val localNotifications = notificationDao.getAllNotifications()
            Log.d("Notifications", "Local notifications fetched: $localNotifications") // Log local notifications

            withContext(Dispatchers.Main) {
                if (localNotifications.isNotEmpty()) {
                    val groupedLocalNotifications = groupNotificationsByDate(localNotifications)
                    Log.d("Notifications", "Grouped local notifications: $groupedLocalNotifications") // Log grouped local notifications

                    setupRecyclerView(groupedLocalNotifications)
                }
            }

            firebaseDatabaseHelper.fetchNotifications { groupedNotifications ->
                lifecycleScope.launch(Dispatchers.Main) {
                    Log.d("Notifications", "Firebase notifications fetched: $groupedNotifications") // Log Firebase notifications

                    if (groupedNotifications.isNotEmpty()) {
                        setupRecyclerView(groupedNotifications)

                        val notificationsToSave = groupedNotifications
                            .filterIsInstance<NotificationItem.NotificationEntry>()
                            .map { it.notification.toLocalNotification() }

                        Log.d("Notifications", "Notifications to save: $notificationsToSave") // Log notifications to be saved

                        lifecycleScope.launch(Dispatchers.IO) {
                            notificationDao.clearNotifications()
                            notificationDao.insertAll(notificationsToSave)
                            Log.d("Notifications", "Notifications saved to local database") // Log saved notifications
                        }
                    } else if (localNotifications.isEmpty()) {
                        Log.d("Notifications", "No notifications available from Firebase or Local DB") // Log if empty
                        Toast.makeText(requireContext(), "No notifications available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView(notifications: List<NotificationItem>) {
        notificationAdapter = RecyclerViewProductNotificationAdapter(notifications) { notification ->
            firebaseDatabaseHelper.fetchItemDetails(notification.itemCode, onSuccess = { item ->
                val intent = Intent(requireContext(), ViewItemDetailsActivity::class.java)
                intent.putExtra("productImg", item.imageUrl)
                intent.putExtra("productName", item.itemName)
                intent.putExtra("productNum", "${item.stocksLeft} unit(s)")
                intent.putExtra("productCode", item.itemCode)
                intent.putExtra("productCategory", item.itemCategory)
                intent.putExtra("productWeight", "${item.itemWeight} g")
                intent.putExtra("productRack", "${item.rackNo}")
                intent.putExtra("productLocation", item.location)
                intent.putExtra("productSupplier", item.supplier)
                intent.putExtra("dateAdded", item.dateAdded)
                intent.putExtra("lastRestocked", item.lastRestocked)
                startActivity(intent)
            }, onFailure = { errorMessage ->
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            })
        }

        notificationRecyclerView.adapter = notificationAdapter
        notificationAdapter.notifyDataSetChanged()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String): Boolean {
                notificationAdapter.filter(newText)
                return true
            }
        })
    }

    fun groupNotificationsByDate(notifications: List<LocalNotification>): List<NotificationItem> {
        Log.d("Notifications", "Grouping notifications: $notifications") // Log input list

        val groupedNotifications = notifications
            .sortedByDescending { it.date }
            .groupBy { it.date }
            .flatMap { (date, notifs) ->
                listOf(NotificationItem.DateHeader(date)) + notifs.map {
                    NotificationItem.NotificationEntry(it.toNotification())
                }
            }

        Log.d("Notifications", "Grouped notifications: $groupedNotifications") // Log grouped list
        return groupedNotifications
    }



}
