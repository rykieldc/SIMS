package com.example.sims

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
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var header: TextView
    private lateinit var firebaseHelper: FirebaseDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_users)

        firebaseHelper = FirebaseDatabaseHelper()

        header = findViewById(R.id.header)

        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_back_arrow_circle)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

        val spannableString = SpannableString("  ${header.text}")
        spannableString.setSpan(
            ImageSpan(drawable!!, DynamicDrawableSpan.ALIGN_BASELINE),
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            DrawableClickSpan { onBackPressedDispatcher.onBackPressed() },
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        header.text = spannableString
        header.movementMethod = LinkMovementMethod.getInstance()

        val addUserButton = findViewById<Button>(R.id.addUserBtn)
        addUserButton.setOnClickListener {
            showAddUserDialog()
        }

        val editUserButton = findViewById<ImageButton>(R.id.editBtn)
        editUserButton.setOnClickListener {
            showEditUserDialog()
        }

        val deleteUserButton = findViewById<ImageButton>(R.id.deleteBtn)
        deleteUserButton.setOnClickListener {
            showDeleteUserDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add User")
            .create()

        val nameEditText = dialogView.findViewById<EditText>(R.id.uploadName)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.uploadUsername)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.uploadRole)

        // Define role options
        val roles = arrayOf("Admin", "User")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

        val saveButton = dialogView.findViewById<Button>(R.id.saveBtn)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelBtn)

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val username = usernameEditText.text.toString()
            val selectedRole = roleSpinner.selectedItem.toString()

            // Assign default password based on the selected role
            val password = if (selectedRole == "Admin") "admin_password" else "user_password"

            if (name.isNotBlank() && username.isNotBlank()) {
                // Save user data to the database
                addUserToDatabase(name, username, password, selectedRole)
                dialog.dismiss() // Close dialog after saving
            } else {
                // Show error if fields are incomplete
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss() // Close dialog on cancel
        }

        dialog.show()
    }

    private fun addUserToDatabase(name: String, username: String, password: String, role: String) {

        firebaseHelper.addUser(username, password, name, role) { success ->
            if (success) {
                Toast.makeText(this, "User added successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error adding user to database. Username may already exist.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_user, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit User")
            .create()

        val saveButton = dialogView.findViewById<Button>(R.id.saveBtn)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelBtn)

        saveButton.setOnClickListener {

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_user, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val yesButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val noButton = dialogView.findViewById<Button>(R.id.noBtn)

        yesButton.setOnClickListener {

            dialog.dismiss()
        }

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}