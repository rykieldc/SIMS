package com.example.sims

import SessionManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfilePage : Fragment() {

    private lateinit var firebaseHelper: FirebaseDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseHelper = FirebaseDatabaseHelper()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userTextView = view.findViewById<TextView>(R.id.profile_name_value)
        val usernameTextView = view.findViewById<TextView>(R.id.profile_username_value)
        val roleTextView = view.findViewById<TextView>(R.id.profile_role_value)

        val savedUsername = SessionManager.getUsername()

        if (!savedUsername.isNullOrEmpty()) {
            firebaseHelper.checkUserData(savedUsername) { user ->
                userTextView.text = user.name
                usernameTextView.text = user.username
                roleTextView.text = user.role
            }
        } else {
            userTextView.text = "Unknown"
            usernameTextView.text = "Unknown"
            roleTextView.text = "Unknown"
        }
    }
}
