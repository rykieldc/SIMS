package com.example.sims

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var contactAdminTextView: TextView
    private lateinit var firebaseHelper: FirebaseDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        contactAdminTextView = findViewById(R.id.contactAdmin)

        firebaseHelper = FirebaseDatabaseHelper()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Log.d("LoginActivity", "Attempting to log in with Username: $username")

                // Check user credentials in Firebase

                firebaseHelper.checkUser(username, password) { isUserValid ->
                    if (isUserValid) {
                        Log.d("LoginActivity", "Login successful for Username: $username")

                        // Assuming you have a way to get the userKey, which may be stored or retrieved from a previous function
                        val userKey =
                            username // Obtain the unique key for the user (e.g., from the login response or another source)

                        firebaseHelper.checkUserData(userKey) { user ->
                            Log.d(
                                "LoginActivity",
                                "Name: ${user.name}, Role: ${user.role}, Username: ${user.username}"
                            )
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("name", user.name)
                            intent.putExtra("username", user.username)
                            intent.putExtra("role", user.role)

                            Log.d("LoginActivity", "Putting role in intent extras: ${user.role}")
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Log.d("LoginActivity", "Login failed for Username: $username")
                        Toast.makeText(
                            this,
                            "Invalid username or password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }
        }

        contactAdminTextView.setOnClickListener {
            val phoneNumber = "09280990649"

            val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(phoneIntent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

