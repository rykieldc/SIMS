package com.example.sims

import SessionManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
            if (!isDestroyed && !isFinishing) {
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            }
            insets
        }


        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        firebaseHelper = FirebaseDatabaseHelper()

        val savedUsername = SessionManager.getUsername()

        if (!savedUsername.isNullOrEmpty()) {
            firebaseHelper.checkUserData(savedUsername) { userData ->
                userRole = userData.role
                if (!isFinishing && !isDestroyed) {
                    setupInitialFragment()
                    setupBottomNavigation()
                } else {
                    Log.w("MainActivity", "Activity is no longer valid; skipping initial setup.")
                }

            }
        } else {
            Log.e("MainActivity", "No saved username found; redirecting to login.")
            startActivity(Intent(this, LoginActivity::class.java))
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
            if (userRole == null) {
                Log.e("MainActivity", "User role is null; skipping fragment replacement.")
                return@setOnItemSelectedListener false
            }

            when (menuItem.itemId) {
                R.id.dashboard -> {
                    val dashboardFragment = if (userRole == "Admin") AdminDashboard() else UserDashboard()
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
        if (!isFinishing && !supportFragmentManager.isDestroyed) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commitAllowingStateLoss()
        } else {
            Log.w("MainActivity", "Skipped replacing fragment; Activity is finishing or FragmentManager is destroyed.")
        }
    }

}