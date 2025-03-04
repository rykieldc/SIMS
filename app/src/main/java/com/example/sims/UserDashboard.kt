package com.example.sims

import SessionManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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

        addItemCardView.setOnClickListener { handleClick(AddItemActivity::class.java) }
        editItemCardView.setOnClickListener { handleClick(EditItemsActivityList::class.java) }
        viewItemCardView.setOnClickListener { startActivity(Intent(requireContext(), ViewItemsActivity::class.java)) }
        deleteItemCardView.setOnClickListener { handleClick(DeleteItemsActivityList::class.java) }
    }

    private fun handleClick(activityClass: Class<*>) {
        if (isInternetAvailable()) {
            startActivity(Intent(requireContext(), activityClass))
        } else {
            Toast.makeText(requireContext(), "No internet connection. Cannot proceed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onResume() {
        super.onResume()

        val savedUsername = SessionManager.getUsername()
        val usernameTextView = view?.findViewById<TextView>(R.id.header_dashboard)

        if (!savedUsername.isNullOrEmpty()) {
            val userDao = App.database.userDao()

            lifecycleScope.launch(Dispatchers.IO) {
                val localUser = userDao.getUserByUsername(savedUsername)

                withContext(Dispatchers.Main) {
                    if (localUser != null) {
                        val displayName = localUser.name
                        usernameTextView?.text = "Hello, $displayName!"
                    } else {
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
