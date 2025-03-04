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
                if (isInternetAvailable()) {
                    // Step 1: Check Firebase login first
                    checkFirebaseLogin(username, password)
                } else {
                    // Step 2: If no internet, check Room database
                    val localUser = checkRoomDatabase(username, password)
                    if (localUser != null) {
                        navigateToMainActivity()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid login. Check internet for first-time login.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                Log.d("LoginDebug", "Found user in Room: ${user.username}")

                if (!user.password.startsWith("$2a$") && !user.password.startsWith("$2b$") && !user.password.startsWith("$2y$")) {
                    Log.e("LoginDebug", "ERROR: Stored password is NOT a valid BCrypt hash!")
                    return@withContext null
                }

                return@withContext if (BCrypt.checkpw(password, user.password)) {
                    Log.d("LoginDebug", "Password match! Logging in...")
                    user
                } else {
                    Log.e("LoginDebug", "Password mismatch!")
                    null
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
                        lifecycleScope.launch(Dispatchers.IO) {
                            val localUser = userDao.getUserByUsername(user.username)

                            if (localUser == null || localUser.password != user.password || localUser.role != user.role || localUser.name != user.name) {
                                Log.d("LoginDebug", "User data changed or new user detected. Updating Room Database...")
                                saveUserToRoom(user)
                            }

                            withContext(Dispatchers.Main) {
                                navigateToMainActivity()
                            }
                        }
                    } else {
                        Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                lifecycleScope.launch {
                    val localUser = checkRoomDatabase(username, password)
                    if (localUser != null) {
                        navigateToMainActivity()
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveUserToRoom(user: User) {
        lifecycleScope.launch(Dispatchers.IO) {
            val hashedPassword = if (user.password.startsWith("$2a$") || user.password.startsWith("$2b$") || user.password.startsWith("$2y$")) {
                Log.d("LoginDebug", "Password already hashed, using as is.")
                user.password
            } else {
                Log.d("LoginDebug", "Hashing password before saving...")
                BCrypt.hashpw(user.password, BCrypt.gensalt())
            }

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
