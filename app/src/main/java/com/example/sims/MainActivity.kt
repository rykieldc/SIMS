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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private var userRole: String? = null
    private val firebaseHelper = FirebaseDatabaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val userDao = App.database.userDao()

        firebaseHelper.listenForStockChanges()

        firebaseHelper.syncUsersToRoom(userDao)
        firebaseHelper.syncItemsToRoom(App.database.itemDao())
        firebaseHelper.syncHistoryToRoom(App.database.historyDao())
        firebaseHelper.syncNotificationsToRoom(App.database.notificationDao())

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            if (!isDestroyed && !isFinishing) {
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            }
            insets
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val savedUsername = SessionManager.getUsername()

        if (!savedUsername.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val localUser = userDao.getUserByUsername(savedUsername)

                withContext(Dispatchers.Main) {
                    if (localUser != null) {
                        userRole = localUser.role
                        setupInitialFragment()
                        setupBottomNavigation()
                    }
                }
            }

            firebaseHelper.checkUserData(savedUsername) { userData ->
                userRole = userData.role
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        setupInitialFragment()
                    }
                }
            }
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
