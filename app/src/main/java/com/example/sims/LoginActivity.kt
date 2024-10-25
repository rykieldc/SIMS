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
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        contactAdminTextView = findViewById(R.id.contactAdmin)

        dbHelper = DatabaseHelper(this)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("LoginActivity", "Attempting to log in with Username: $username")

                // Check for specific credentials
                val intent = Intent(this, MainActivity::class.java)
                if (username == "user" && password == "user_password") {
                    Log.d("LoginActivity", "Login successful for User account")
                    intent.putExtra("role", "user") // Pass "user" role
                    startActivity(intent)
                    finish()
                } else if (username == "admin" && password == "admin_password") {
                    Log.d("LoginActivity", "Login successful for Admin account")
                    intent.putExtra("role", "admin") // Pass "admin" role
                    startActivity(intent)
                    finish()
                } else {
                    Log.d("LoginActivity", "Login failed for Username: $username")
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()

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
