package com.example.sims

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class UserDashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find card views by their IDs
        val addItemCardView = view.findViewById<CardView>(R.id.cvAddItem)
        val editItemCardView = view.findViewById<CardView>(R.id.cvEditItem)
        val viewItemCardView = view.findViewById<CardView>(R.id.cvViewItem)
        val deleteItemCardView = view.findViewById<CardView>(R.id.cvDeleteItem)

        // Set click listeners for each card view to start the corresponding activity
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
    }
}
