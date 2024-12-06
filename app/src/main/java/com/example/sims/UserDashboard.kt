package com.example.sims

import SessionManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class UserDashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addItemCardView = view.findViewById<CardView>(R.id.cvAddItem)
        val editItemCardView = view.findViewById<CardView>(R.id.cvEditItem)
        val viewItemCardView = view.findViewById<CardView>(R.id.cvViewItem)
        val deleteItemCardView = view.findViewById<CardView>(R.id.cvDeleteItem)

        addItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), AddItemActivity::class.java)
            startActivity(intent)
        }

        editItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), EditItemsActivityList::class.java)
            startActivity(intent)
        }

        viewItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), ViewItemsActivity::class.java)
            startActivity(intent)
        }

        deleteItemCardView.setOnClickListener {
            val intent = Intent(requireContext(), DeleteItemsActivityList::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        val userName = SessionManager.getUsername()
        val usernameTextView = view?.findViewById<TextView>(R.id.header_dashboard)
        usernameTextView?.text = "Hello, $userName!"
    }
}
