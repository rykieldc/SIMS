package com.example.sims

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Notifications : Fragment() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: RecyclerViewProductNotificationAdapter
    private lateinit var searchView: SearchView
    private val firebaseDatabaseHelper = FirebaseDatabaseHelper()
    private var originalNotificationList = mutableListOf<ProductNotifications>()

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

        firebaseDatabaseHelper.fetchNotifications { notificationsList ->
            val productNotificationsList = notificationsList.map { firebaseNotification ->
                ProductNotifications(
                    itemCode = firebaseNotification.itemCode,
                    date = firebaseNotification.date,
                    icon = firebaseNotification.icon,
                    details = firebaseNotification.details,
                    enabled = firebaseNotification.enabled
                )
            }
            originalNotificationList = productNotificationsList.toMutableList()

            notificationAdapter = RecyclerViewProductNotificationAdapter(productNotificationsList) { notification ->
                firebaseDatabaseHelper.fetchItemDetails(notification.itemCode, onSuccess = { item ->
                    val intent = Intent(requireContext(), ViewItemDetailsActivity::class.java)
                    intent.putExtra("productImg", item.imageUrl)
                    intent.putExtra("productName", item.itemName)
                    intent.putExtra("productNum", "${item.stocksLeft} unit(s)")
                    intent.putExtra("productCode", item.itemCode)
                    intent.putExtra("productCategory", item.itemCategory)
                    intent.putExtra("productWeight", "${item.itemWeight} g")
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
        }
    }


    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                notificationAdapter.filter(newText)
                return true
            }
        })
    }
}