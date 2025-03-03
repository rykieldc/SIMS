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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val savedUsername = SessionManager.getUsername()
        val usernameTextView = view?.findViewById<TextView>(R.id.header_dashboard)

        if (!savedUsername.isNullOrEmpty()) {
            val userDao = App.database.userDao()

            lifecycleScope.launch(Dispatchers.IO) {
                // Try to get user data from Room Database first
                val localUser = userDao.getUserByUsername(savedUsername)

                withContext(Dispatchers.Main) {
                    if (localUser != null) {
                        val displayName = localUser.name
                        usernameTextView?.text = "Hello, $displayName!"
                    } else {
                        // If no local data, fetch from Firestore
                        FirebaseDatabaseHelper().checkUserData(savedUsername) { user ->
                            val displayName = user.name
                            usernameTextView?.text = "Hello, $displayName!"
                        }
                    }
                }
            }
        } else {
            usernameTextView?.text = "Hello, !"
        }
    }
}
