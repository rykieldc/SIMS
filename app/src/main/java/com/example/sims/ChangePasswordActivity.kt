package com.example.sims

import SessionManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText


class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var header: TextView
    private var userName: String? = null
    private lateinit var cPasswordEditText: TextInputEditText
    private lateinit var nPasswordEditText: TextInputEditText
    private lateinit var cNewPasswordEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

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

        val saveButton = findViewById<Button>(R.id.saveBtn)
        saveButton.setOnClickListener {
            showSaveConfirmationDialog()
        }

        val cancelButton = findViewById<Button>(R.id.cancelBtn)
        cancelButton.setOnClickListener {
            showCancelConfirmationDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userName = SessionManager.getUsername()
        cPasswordEditText = findViewById(R.id.currentPasswordEditText)
        nPasswordEditText = findViewById(R.id.newPasswordEditText)
        cNewPasswordEditText = findViewById(R.id.confirmPasswordEditText)

    }

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }

    private fun showSaveConfirmationDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_save_changes, null)
        val saveButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val cancelButton = dialogView.findViewById<Button>(R.id.noBtn)



        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            changePassword()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun changePassword() {
        val currentPassword = cPasswordEditText.text.toString().trim()
        val newPassword = nPasswordEditText.text.toString().trim()
        val confirmPassword = cNewPasswordEditText.text.toString().trim()

        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (!newPassword.any { it.isDigit() }) {
            Toast.makeText(this, "Password must contain at least one number", Toast.LENGTH_SHORT).show()
            return
        }


        if (newPassword != confirmPassword) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val databaseHelper = FirebaseDatabaseHelper()
        databaseHelper.changeUserPassword(userName!!, currentPassword, newPassword) { success ->
            if (success) {
                Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Incorrect Current Password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to show the cancel confirmation dialog
    private fun showCancelConfirmationDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_cancel, null)
        val yesButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val noButton = dialogView.findViewById<Button>(R.id.noBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        yesButton.setOnClickListener {
            // Handle cancel action here
            // e.g., discard changes, close activity, etc.
            dialog.dismiss()
            finish() // Close the activity
        }

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}