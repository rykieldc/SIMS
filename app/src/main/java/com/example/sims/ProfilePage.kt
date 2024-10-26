package com.example.sims

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView

class ProfilePage : Fragment() {

    private var user: String? = null
    private var userName: String? = null
    private var userRole: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        user = arguments?.getString("name")
        userName = arguments?.getString("username")
        userRole = arguments?.getString("role")

        Log.d("ProfilePage", "User: $user, Username: $userName, Role: $userRole")

        val userTextView = view.findViewById<TextView>(R.id.profile_name_value)
        userTextView.text =  user

        val usernameTextView = view.findViewById<TextView>(R.id.profile_username_value)
        usernameTextView.text =  userName

        val roleTextView = view.findViewById<TextView>(R.id.profile_role_value)
        roleTextView.text =  userRole


    }
}