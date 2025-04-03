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

class StatisticsDashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ItemCardView = view.findViewById<CardView>(R.id.cvItem)
        val AlertCardView = view.findViewById<CardView>(R.id.cvAlert)
        val TopCardView = view.findViewById<CardView>(R.id.cvTop)
        val BarCardView = view.findViewById<CardView>(R.id.cvBar)

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
