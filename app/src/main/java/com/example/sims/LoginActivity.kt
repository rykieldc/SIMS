package com.example.sims

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var contactAdminTextView: TextView
    private lateinit var firebaseHelper: FirebaseDatabaseHelper
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        contactAdminTextView = findViewById(R.id.contactAdmin)

        firebaseHelper = FirebaseDatabaseHelper()
        userDao = App.database.userDao()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val localUser = checkRoomDatabase(username, password)

                if (localUser != null) {
                    navigateToMainActivity()
                } else if (isInternetAvailable()) {
                    checkFirebaseLogin(username, password)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Invalid login. Check internet for first-time login.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        contactAdminTextView.setOnClickListener {
            val phoneNumber = "09280990649"
            val phoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            startActivity(phoneIntent)
        }
    }

    private suspend fun checkRoomDatabase(username: String, password: String): LocalUser? {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByUsername(username)
            if (user != null) {
                Log.d("LoginDebug", "Found user in Room: $user")
                Log.d("LoginDebug", "Entered password: $password, Stored password: ${user.password}")

                if (BCrypt.checkpw(password, user.password)) {
                    Log.d("LoginDebug", "Password match! Logging in...")
                    return@withContext user
                } else {
                    Log.d("LoginDebug", "Password mismatch!")
                }
            } else {
                Log.d("LoginDebug", "User not found in Room!")
            }
            null
        }
    }


    private fun checkFirebaseLogin(username: String, password: String) {
        firebaseHelper.checkUser(username, password) { isUserValid ->
            if (isUserValid) {
                firebaseHelper.checkUserData(username) { user ->
                    if (user.username.isNotEmpty()) {
                        saveUserToRoom(user)

                        navigateToMainActivity()
                    } else {
                        Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserToRoom(user: User) {
        lifecycleScope.launch(Dispatchers.IO) {
            val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())

            Log.d("LoginDebug", "Saving user to Room: ${user.username}, Hashed Password: $hashedPassword")

            userDao.insert(
                LocalUser(
                    username = user.username,
                    password = hashedPassword,
                    name = user.name,
                    role = user.role,
                    enabled = user.enabled
                )
            )

            Log.d("LoginDebug", "User successfully saved in Room!")
        }
    }



    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
