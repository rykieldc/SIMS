package com.example.sims

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Notifications : Fragment() {

    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: RecyclerViewProductNotificationAdapter

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

        val notificationData = getNotificationData()
        notificationAdapter = RecyclerViewProductNotificationAdapter(notificationData)
        notificationRecyclerView.adapter = notificationAdapter
    }

    private fun getNotificationData(): List<ProductNotifications> {
        return listOf(
            ProductNotifications(R.drawable.ic_important, getString(R.string.sample_notif), "low"),
            ProductNotifications(R.drawable.ic_high_priority, getString(R.string.sample_notif4), "critical")
        )
    }

}