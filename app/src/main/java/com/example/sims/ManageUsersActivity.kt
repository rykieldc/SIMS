// ManageUsersActivity.kt
package com.example.sims

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var firebaseHelper: FirebaseDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        firebaseHelper = FirebaseDatabaseHelper()

        val editUserButton = findViewById<ImageButton>(R.id.editBtn)
        editUserButton.setOnClickListener {
            showEditUserDialog("username_to_edit") // replace with actual username
        }

        val deleteUserButton = findViewById<ImageButton>(R.id.deleteBtn)
        deleteUserButton.setOnClickListener {
            showDeleteUserDialog("username_to_delete") // replace with actual username
        }
    }

    private fun showEditUserDialog(username: String) {
        firebaseHelper.getUser(username) { user ->
            if (user != null) {
                val dialogView = layoutInflater.inflate(R.layout.dialog_edit_user, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setTitle("Edit User")
                    .create()

                val nameEditText = dialogView.findViewById<EditText>(R.id.editName)
                val roleSpinner = dialogView.findViewById<Spinner>(R.id.UploadRole)

                nameEditText.setText(user.name)
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Admin", "User"))
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                roleSpinner.adapter = adapter
                roleSpinner.setSelection(if (user.role == "Admin") 0 else 1)

                val saveButton = dialogView.findViewById<Button>(R.id.saveBtn)
                saveButton.setOnClickListener {
                    val updatedName = nameEditText.text.toString()
                    val updatedRole = roleSpinner.selectedItem.toString()
                    val updatedUser = User(username, user.password, updatedName, updatedRole)

                    firebaseHelper.updateUser(username, updatedUser) { success ->
                        if (success) {
                            Toast.makeText(this, "User updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to update user.", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                }

                val cancelButton = dialogView.findViewById<Button>(R.id.cancelBtn)
                cancelButton.setOnClickListener { dialog.dismiss() }

                dialog.show()
            } else {
                Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteUserDialog(username: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_user, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Delete User")
            .create()

        val yesButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val noButton = dialogView.findViewById<Button>(R.id.noBtn)

        yesButton.setOnClickListener {
            firebaseHelper.deleteUser(username) { success ->
                if (success) {
                    Toast.makeText(this, "User deleted successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete user.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        noButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
