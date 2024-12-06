package com.example.sims

import SessionManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var firebaseHelper: FirebaseDatabaseHelper
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        firebaseHelper = FirebaseDatabaseHelper()

        val savedUsername = SessionManager.getUsername()

        if (!savedUsername.isNullOrEmpty()) {
            firebaseHelper.checkUserData(savedUsername) { userData ->
                userRole = userData.role
                setupInitialFragment()
                setupBottomNavigation()
            }
        } else {
            finish()
        }
    }

    private fun setupInitialFragment() {
        val initialFragment = if (userRole == "Admin") {
            AdminDashboard()
        } else {
            UserDashboard()
        }
        replaceFragment(initialFragment)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.dashboard -> {
                    val dashboardFragment = if (userRole == "Admin") {
                        AdminDashboard()
                    } else {
                        UserDashboard()
                    }
                    replaceFragment(dashboardFragment)
                    true
                }

                R.id.notifications -> {
                    replaceFragment(Notifications())
                    true
                }

                R.id.profile -> {
                    replaceFragment(ProfilePage())
                    true
                }

                R.id.settings -> {
                    replaceFragment(Settings())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frameLayout, fragment).commit()
    }
}