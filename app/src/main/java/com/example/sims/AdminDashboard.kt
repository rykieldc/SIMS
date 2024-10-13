package com.example.sims

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class AdminDashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addItemCardView = view.findViewById<CardView>(R.id.cvAddItem)
        val editItemCardView = view.findViewById<CardView>(R.id.cvEditItem)
        val viewItemCardView = view.findViewById<CardView>(R.id.cvViewItem)
        val deleteItemCardView = view.findViewById<CardView>(R.id.cvDeleteItem)
        val manageUsersCardView = view.findViewById<CardView>(R.id.cvManageUsers)
        val historyCardView = view.findViewById<CardView>(R.id.cvHistory)


        addItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), AddItemActivity::class.java)
            startActivity(intent)
        }

        editItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), EditItemActivity::class.java)
            startActivity(intent)
        }

        viewItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), ViewItemsActivity::class.java)
            startActivity(intent)
        }

        deleteItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), ViewItemDetailsActivity::class.java)
            startActivity(intent)
        }

        manageUsersCardView.setOnClickListener {
            val intent = Intent(requireContext(), ManageUsersActivity::class.java)
            startActivity(intent)
        }

        historyCardView.setOnClickListener {
            val intent = Intent(requireContext(), UserLogsActivity::class.java)
            startActivity(intent)
        }

    }
}