package com.example.sims

import SessionManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilePage : Fragment() {

    private lateinit var firebaseHelper: FirebaseDatabaseHelper
    private lateinit var userTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var roleTextView: TextView
    private lateinit var profileHeader: TextView

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

        userTextView = view.findViewById(R.id.profile_name_value)
        usernameTextView = view.findViewById(R.id.profile_username_value)
        roleTextView = view.findViewById(R.id.profile_role_value)
        profileHeader = view.findViewById(R.id.header_profile)

        addIconToHeader()

        val savedUsername = SessionManager.getUsername()

        if (!savedUsername.isNullOrEmpty()) {
            if (isInternetAvailable()) {
                fetchUserFromFirestore(savedUsername)
            } else {
                fetchUserFromLocalDatabase(savedUsername)
            }
        } else {
            displayUnknownUser()
        }
    }

    private fun fetchUserFromFirestore(username: String) {
        firebaseHelper.checkUserData(username) { user ->
            if (user != null) {
                updateUI(user.name, user.username, user.role)

                lifecycleScope.launch(Dispatchers.IO) {
                    val userDao = App.database.userDao()
                    userDao.insert(
                        LocalUser(
                            name = user.name,
                            username = user.username,
                            role = user.role,
                            enabled = user.enabled,
                            password = user.password
                        )
                    )
                }
            } else {
                displayUnknownUser()
            }
        }
    }


    private fun fetchUserFromLocalDatabase(username: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userDao = App.database.userDao()
            val localUser = userDao.getUserByUsername(username)

            withContext(Dispatchers.Main) {
                if (localUser != null) {
                    updateUI(localUser.name, localUser.username, localUser.role)
                } else {
                    displayUnknownUser()
                }
            }
        }
    }

    private fun updateUI(name: String, username: String, role: String) {
        userTextView.text = name
        usernameTextView.text = username
        roleTextView.text = role
    }

    private fun displayUnknownUser() {
        userTextView.text = "Unknown"
        usernameTextView.text = "Unknown"
        roleTextView.text = "Unknown"
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    private fun addIconToHeader() {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_back_arrow_circle)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

        val spannableString = SpannableString("  ${profileHeader.text}")
        spannableString.setSpan(
            ImageSpan(drawable!!, DynamicDrawableSpan.ALIGN_BASELINE),
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            DrawableClickSpan {
                (activity as? MainActivity)?.replaceFragment(Settings()) // Replace with ProfilePage or your desired Fragment
            },
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        profileHeader.text = spannableString
        profileHeader.movementMethod = LinkMovementMethod.getInstance()
    }


    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }
}
