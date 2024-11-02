package com.example.sims

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
    private var userRole: String? = null
    private var userName: String? = null
    private var user: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val dbHelper = FirebaseDatabaseHelper()
        dbHelper.fetchCategories { success ->
            if (success) {
                Log.d("MainActivity", "Categories loaded successfully")
            } else {
                Log.e("MainActivity", "Failed to load categories")
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        user = intent.getStringExtra("name")
        userName = intent.getStringExtra("username")
        userRole = intent.getStringExtra("role")



        if (userRole == "Admin") {
            val adminDashboard = AdminDashboard().apply {
                arguments = Bundle().apply {
                    putString("user", user)
                    putString("username", userName)
                    putString("role", userRole)
                }
            }
            replaceFragment(adminDashboard)
        } else {
            val userDashboard = UserDashboard().apply {
                arguments = Bundle().apply {
                    putString("user", user)
                    putString("username", userName)
                    putString("role", userRole)
                }
            }
            replaceFragment(userDashboard)
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.dashboard -> {
                    if (userRole == "Admin") {
                        val adminDashboard = AdminDashboard().apply {
                            arguments = Bundle().apply {
                                putString("user", user)
                                putString("username", userName)
                                putString("role", userRole)
                            }
                        }
                        replaceFragment(adminDashboard)
                    } else {
                        val userDashboard = UserDashboard().apply {
                            arguments = Bundle().apply {
                                putString("user", user)
                                putString("username", userName)
                                putString("role", userRole)
                            }
                        }
                        replaceFragment(userDashboard)
                    }
                    true
                }

                R.id.notifications -> {
                    replaceFragment(Notifications())
                    true
                }

                R.id.profile -> {
                    val profilePage = ProfilePage()
                    val bundle = Bundle()
                    bundle.putString("name", user)
                    bundle.putString("username", userName)
                    bundle.putString("role", userRole)
                    profilePage.arguments = bundle

                    replaceFragment(profilePage)
                    true
                }

                R.id.settings -> {
                    val settingsFragment = Settings().apply {
                        arguments = Bundle().apply {
                            putString("username", userName)
                        }
                    }
                    replaceFragment(settingsFragment)
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


