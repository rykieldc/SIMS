package com.example.sims

import SessionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
class RecyclerViewUsersAdapter(
    private val getActivity: ManageUsersActivity,
    private val usersList: MutableList<User> = mutableListOf()
) : RecyclerView.Adapter<RecyclerViewUsersAdapter.UsersViewHolder>() {

    var originalList = usersList.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_users, parent, false)
        return UsersViewHolder(view)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = usersList[position]
        (position + 1).toString().also { holder.userIndex.text = it }
        holder.userName.text = user.name
        holder.userRole.text = user.role

        holder.editButton.setOnClickListener {
            showEditUserDialog(user, position)
        }

        holder.deleteButton.setOnClickListener {
            showDeleteUserDialog(user, position)
        }
    }

    class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userIndex: TextView = itemView.findViewById(R.id.userId)
        val userName: TextView = itemView.findViewById(R.id.userName)
        val userRole: TextView = itemView.findViewById(R.id.userRole)
        val editButton: ImageButton = itemView.findViewById(R.id.editBtn)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteBtn)
    }

    fun filter(query: String) {
        val filteredList = originalList.filter { user ->
            user.name.lowercase().contains(query.lowercase()) ||
            user.role.lowercase().contains(query.lowercase())

        }

        updateUsersList(filteredList)
    }

    fun resetList() {
        updateUsersList(originalList)
    }

    private fun updateUsersList(newList: List<User>) {
        val diffCallback = LogDiffCallback(usersList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        usersList.clear()
        usersList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    class LogDiffCallback(
        private val oldList: List<User>,
        private val newList: List<User>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].username == newList[newItemPosition].username

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }


    private fun showEditUserDialog(user: User, position: Int) {
        val dialogView = LayoutInflater.from(getActivity).inflate(R.layout.dialog_edit_user, null)

        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editUsername = dialogView.findViewById<EditText>(R.id.editUsername)
        val editRole = dialogView.findViewById<Spinner>(R.id.UploadRole)

        editName.setText(user.name)
        editUsername.setText(user.username)

        val roles = arrayOf("Admin", "User")
        val adapter = android.widget.ArrayAdapter(getActivity, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        editRole.adapter = adapter
        
        val roleIndex = roles.indexOf(user.role)
        if (roleIndex >= 0) {
            editRole.setSelection(roleIndex)
        }

        val dialog = AlertDialog.Builder(getActivity)
            .setView(dialogView)
            .setTitle("Edit User")
            .create()

        dialogView.findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val selectedRole = editRole.selectedItem.toString()
            showSaveConfirmationDialog(
                user,
                editName.text.toString(),
                editUsername.text.toString(),
                selectedRole,
                dialog,
                position
            )
        }

        dialogView.findViewById<Button>(R.id.cancelBtn).setOnClickListener {
            showCancelConfirmationDialog(dialog)
        }

        dialog.show()
    }


    private fun showSaveConfirmationDialog(user: User, updatedName: String, updatedUsername: String, updatedRole: String, dialog: AlertDialog, position: Int) {
        val dialogView = LayoutInflater.from(getActivity).inflate(R.layout.dialog_save_changes, null)
        val saveButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val cancelButton = dialogView.findViewById<Button>(R.id.noBtn)

        val saveDialog = AlertDialog.Builder(getActivity)
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            val updatedUser = user.copy(
                name = updatedName,
                username = updatedUsername,
                role = updatedRole
            )

            updateUserInDatabase(updatedUser, dialog, position)
            dialog.dismiss()
            saveDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            saveDialog.dismiss()
        }

        saveDialog.show()
    }

    private fun showCancelConfirmationDialog(dialog: AlertDialog) {
        val dialogView = LayoutInflater.from(getActivity).inflate(R.layout.dialog_cancel, null)
        val yesButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val noButton = dialogView.findViewById<Button>(R.id.noBtn)

        val confirmationDialog = AlertDialog.Builder(getActivity)
            .setView(dialogView)
            .create()

        yesButton.setOnClickListener {
            dialog.dismiss()
            confirmationDialog.dismiss()
        }

        noButton.setOnClickListener {
            confirmationDialog.dismiss()
        }

        confirmationDialog.show()
    }


    private fun updateUserInDatabase(user: User, dialog: AlertDialog, position: Int) {
        val oldUser = usersList[position]
        val oldUsername = oldUser.username
        val newUsername = user.username
        val dbHelper = FirebaseDatabaseHelper()

        val isUsernameExists = usersList.any { it.username == newUsername && it.username != oldUsername }

        if (isUsernameExists) {
            Toast.makeText(getActivity, "Username already exists. Please choose a different username.", Toast.LENGTH_SHORT).show()
            return
        }

        val userDetails = StringBuilder()

        if (oldUsername != newUsername) {
            dbHelper.deleteUser(oldUsername, onSuccess = {
                dbHelper.addUser(newUsername, user, onSuccess = {
                    if (SessionManager.getUsername() == oldUsername) {
                        SessionManager.saveUsername(newUsername)
                    }

                    userDetails.append("Updated Username from [$oldUsername] to [$newUsername]. ")

                    if (oldUser.name != user.name) {
                        userDetails.append("Updated Name from [${oldUser.name}] to [${user.name}]. ")
                    }
                    if (oldUser.role != user.role) {
                        userDetails.append("Updated Role from [${oldUser.role}] to [${user.role}]. ")
                    }
                    if (oldUser.enabled != user.enabled) {
                        userDetails.append("Updated Status from [${oldUser.enabled}] to [${user.enabled}]. ")
                    }

                    val date = SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date())
                    val action = "Updated User [$newUsername]"

                    dbHelper.recordUserHistory(date, action, user, userDetails.toString()) { historySuccess ->
                        if (historySuccess) {
                            Toast.makeText(getActivity, "User updated successfully", Toast.LENGTH_SHORT).show()
                            notifyItemChanged(position)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(getActivity, "Failed to record history", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, onFailure = { errorMessage ->
                    Toast.makeText(getActivity, errorMessage, Toast.LENGTH_SHORT).show()
                })
            }, onFailure = { errorMessage ->
                Toast.makeText(getActivity, errorMessage, Toast.LENGTH_SHORT).show()
            })
        }  else {
            dbHelper.updateUser(newUsername, user, onSuccess = {
                if (SessionManager.getUsername() == oldUsername) {
                    SessionManager.saveUsername(newUsername)
                }

                Toast.makeText(getActivity, "User updated successfully", Toast.LENGTH_SHORT).show()
                notifyItemChanged(position)
                dialog.dismiss()
            }, onFailure = { errorMessage ->
                Toast.makeText(getActivity, errorMessage, Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun showDeleteUserDialog(user: User, position: Int) {
        val dialogView = LayoutInflater.from(getActivity).inflate(R.layout.dialog_delete_user, null)
        val dialog = AlertDialog.Builder(getActivity)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.yesBtn).setOnClickListener {
            val dbHelper = FirebaseDatabaseHelper()

            dbHelper.setUserEnabled(user.username, false) { success ->
                if (success) {
                    Toast.makeText(getActivity, "${user.name} has been deleted.", Toast.LENGTH_SHORT).show()

                    getActivity.lifecycleScope.launch {
                        getActivity.fetchUsersFromDatabase()
                    }

                } else {
                    Toast.makeText(getActivity, "Failed to delete user.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }


        dialogView.findViewById<Button>(R.id.noBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}