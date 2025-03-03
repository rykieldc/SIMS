package com.example.sims

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var header: TextView
    private lateinit var searchView: SearchView
    private lateinit var firebaseHelper: FirebaseDatabaseHelper
    private var recyclerView: RecyclerView? = null
    private var recyclerViewUsersAdapter: RecyclerViewUsersAdapter? = null
    private var userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        firebaseHelper = FirebaseDatabaseHelper()

        header = findViewById(R.id.header)
        setupHeaderWithBackIcon()

        recyclerView = findViewById(R.id.rvViewUsers)
        recyclerViewUsersAdapter = RecyclerViewUsersAdapter(this@ManageUsersActivity, userList)

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(this@ManageUsersActivity)
            adapter = recyclerViewUsersAdapter
        }

        setupSearchView()
        lifecycleScope.launch { fetchUsersFromDatabase() }

        findViewById<Button>(R.id.addUserBtn).setOnClickListener { showAddUserDialog() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupHeaderWithBackIcon() {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_back_arrow_circle)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val spannableString = SpannableString("  ${header.text}").apply {
            setSpan(ImageSpan(drawable!!, DynamicDrawableSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(DrawableClickSpan { onBackPressedDispatcher.onBackPressed() }, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        header.text = spannableString
        header.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.searchProduct)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    recyclerViewUsersAdapter?.resetList()
                } else {
                    recyclerViewUsersAdapter?.filter(newText)
                }
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { fetchUsersFromDatabase() }
        searchView.setQuery("", false)
        searchView.clearFocus()
    }




    @SuppressLint("NotifyDataSetChanged")
    suspend fun fetchUsersFromDatabase() {
        withContext(Dispatchers.IO) {
            val userDao = App.database.userDao()
            val localUsers = userDao.getAllUsers()

            if (localUsers.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    userList.clear()
                    userList.addAll(localUsers.map { localUser ->
                        User(localUser.name, localUser.username, localUser.role,
                            localUser.enabled.toString()
                        )
                    })
                    recyclerViewUsersAdapter?.originalList = userList.toMutableList()
                    recyclerViewUsersAdapter?.notifyDataSetChanged()
                }
            }

            if (isInternetAvailable()) {
                firebaseHelper.fetchUsers { fetchedUsers ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (fetchedUsers.isNotEmpty()) {
                            userDao.insertAll(fetchedUsers.map { user ->
                                LocalUser(user.name, user.username, user.role, user.enabled, "default_password")
                            })

                            withContext(Dispatchers.Main) {
                                userList.clear()
                                userList.addAll(fetchedUsers.filter { it.enabled })
                                recyclerViewUsersAdapter?.originalList = userList.toMutableList()
                                recyclerViewUsersAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }


    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }



    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setTitle("Add User").create()

        val nameEditText = dialogView.findViewById<EditText>(R.id.uploadName)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.uploadUsername)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.uploadRole)

        val roles = arrayOf("Admin", "User")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleSpinner.adapter = adapter

        dialogView.findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val name = nameEditText.text.toString()
            val username = usernameEditText.text.toString()
            val selectedRole = roleSpinner.selectedItem.toString()
            val password = if (selectedRole == "Admin") "admin_password" else "user_password"

            if (name.isNotBlank() && username.isNotBlank()) {
                val isDuplicateUsername = userList.any { it.username == username }

                if (isDuplicateUsername) {
                    Toast.makeText(this, "Username already exists. Please choose a different username.", Toast.LENGTH_SHORT).show()
                } else {
                    addUserToDatabase(name, username, password, selectedRole)
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<Button>(R.id.cancelBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun addUserToDatabase(name: String, username: String, password: String, role: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userDao = App.database.userDao()
            val newUser = LocalUser(name, username, role, enabled = true, password)

            userDao.insert(newUser)

            withContext(Dispatchers.Main) {
                userList.add(User(name, username, role, enabled = true))
                recyclerViewUsersAdapter?.notifyDataSetChanged()
            }

            if (isInternetAvailable()) {
                firebaseHelper.addUser(username, password, name, role) { success ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@ManageUsersActivity,
                            if (success) "User added successfully!" else "Error adding user to Firestore.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ManageUsersActivity,
                        "User saved locally. Will sync when online.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }



    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }
}
