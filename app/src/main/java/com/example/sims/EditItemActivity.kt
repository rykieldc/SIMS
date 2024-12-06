package com.example.sims

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditItemActivity : AppCompatActivity() {
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper
    private lateinit var headerProduct: TextView
    private lateinit var editImg: ImageView
    private lateinit var editName: EditText
    private lateinit var editUnits: EditText
    private lateinit var editCode: EditText
    private lateinit var editCategory: EditText
    private lateinit var editLocation: Spinner
    private lateinit var editSupplier: EditText
    private lateinit var editDateAdded: EditText
    private lateinit var editLastRestocked: EditText
    private lateinit var imageChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var originalProductCode: String
    private lateinit var originalProductCategory: String
    private var originalStocksLeft: Int = 0

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var driveService: Drive

    private val driveScopes = listOf("https://www.googleapis.com/auth/drive.file")
    private val calendar: Calendar = Calendar.getInstance()
    private var imageUri: Uri? = null

    private var isLocationSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_item)

        headerProduct = findViewById(R.id.header)

        editImg = findViewById(R.id.editImg)
        editName = findViewById(R.id.editName)
        editUnits = findViewById(R.id.editUnits)
        editCode = findViewById(R.id.editCode)
        editCategory = findViewById(R.id.editCategory)
        editLocation = findViewById(R.id.editLocation)
        editSupplier = findViewById(R.id.editSupplier)
        editDateAdded = findViewById(R.id.editDateAdded)
        editLastRestocked = findViewById(R.id.editLastRestocked)
        firebaseDatabaseHelper = FirebaseDatabaseHelper()

        editCode.isEnabled = false
        editCategory.isEnabled = false
        editDateAdded.isEnabled = false
        editLastRestocked.isEnabled = false

        imageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                imageUri = data?.data
                editImg.setImageURI(imageUri)
            }
        }

        editUnits.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val currentText = editUnits.text.toString()
                if (currentText.endsWith(" units")) {
                    val unitsText = currentText.replace(" units", "")
                    editUnits.setText(unitsText)
                    editUnits.setSelection(unitsText.length)
                }
            }
        }

        setupSpinners()
        setupHeader()
        setupImageChooser()
        setupDatePickers()
        setupGoogleSignIn()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val locations = arrayOf(
            "Store Front", "Store Stock Room", "Porta Vaga Stock Room",
            "YMCA Stock Room", "Home"
        )

        originalProductCode = intent.getStringExtra("productCode") ?: ""
        originalProductCategory = intent.getStringExtra("productCategory") ?: ""

        intent.getStringExtra("productImg")?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_img_placeholder)
                .error(R.drawable.ic_img_placeholder)
                .into(editImg)
        }
        editName.setText(intent.getStringExtra("productName"))
        editUnits.setText(intent.getStringExtra("productNum"))
        editCode.setText(intent.getStringExtra("productCode"))
        editCategory.setText(intent.getStringExtra("productCategory"))
        editSupplier.setText(intent.getStringExtra("productSupplier"))
        editDateAdded.setText(intent.getStringExtra("dateAdded"))
        editLastRestocked.setText(intent.getStringExtra("lastRestocked"))

        originalStocksLeft = intent.getStringExtra("productNum")?.toIntOrNull() ?: 0


        intent.getStringExtra("productLocation")?.let { location ->
            val locationPosition = locations.indexOf(location)
            if (locationPosition != -1) {
                editLocation.setSelection(locationPosition)
            }
        }

        val saveButton = findViewById<Button>(R.id.saveBtn)
        saveButton.setOnClickListener {
            if (validateInputs()) {
                showSaveConfirmationDialog()
            }
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
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(Scopes.DRIVE_FILE),
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signInIntent.also {
            startActivityForResult(it, 100)
        }
    }

    private var isDriveServiceInitialized = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener { googleAccount ->
                    val credential = GoogleAccountCredential.usingOAuth2(this, driveScopes)
                    credential.selectedAccount = googleAccount.account

                    driveService = Drive.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential
                    ).setApplicationName("SimsApp").build()

                    isDriveServiceInitialized = true
                    Log.d("GoogleSignIn", "Drive service initialized successfully.")
                    showToast("Google Drive service is initialized.")
                }
                .addOnFailureListener { e ->
                    if (e.message?.contains("insufficientScopes") == true) {
                        showToast("Insufficient permissions. Please sign in again.")
                        setupGoogleSignIn()
                    } else {
                        showToast("Sign-in failed. Try again.")
                    }
                }

        }
    }

    private fun validateInputs(): Boolean {
        if (editName.text.isNullOrEmpty()) {
            showToast("Please enter the product name.")
            return false
        }

        if (editUnits.text.isNullOrEmpty()) {
            showToast("Please enter the units.")
            return false
        }

        if (editSupplier.text.isNullOrEmpty()) {
            showToast("Please enter the supplier.")
            return false
        }

        if (editCode.text.isNullOrEmpty()) {
            showToast("Please enter the product code.")
            return false
        }

        if (editCategory.text.isNullOrEmpty()) {
            showToast("Please select a category.")
            return false
        }

        if (!isLocationSelected) {
            showToast("Please select a location.")
            return false
        }

        if (editDateAdded.text.isNullOrEmpty()) {
            showToast("Please enter the date added.")
            return false
        }

        if (editLastRestocked.text.isNullOrEmpty()) {
            showToast("Please enter the last restocked date.")
            return false
        }

        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun openImageChooser(view: View) {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        imageChooserLauncher.launch(intent)
    }

    private fun showSaveConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_changes, null)
        val saveButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val cancelButton = dialogView.findViewById<Button>(R.id.noBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            if (validateInputs()) {
                uploadImageToDrive()
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uploadImageToDrive() {
        if (!isDriveServiceInitialized) {
            showToast("Google Drive service is not initialized. Please try again.")
            return
        }

        Thread {
            try {
                val imageLink = if (imageUri != null) {
                    val contentResolver = contentResolver
                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    val metadata = File().apply {
                        name = "uploaded_image.jpg"
                        parents = listOf("root")
                    }

                    val fileContent = InputStreamContent("image/jpeg", inputStream)
                    val driveFile = driveService.files().create(metadata, fileContent).setFields("id").execute()
                    val fileId = driveFile.id

                    val permission = Permission().apply {
                        role = "reader"
                        type = "anyone"
                    }
                    driveService.permissions().create(fileId, permission).execute()
                    "https://drive.google.com/uc?id=$fileId"
                } else {
                    intent.getStringExtra("productImg") ?: "https://drive.google.com/uc?id=1ly0Agk51NxmWUj_GUN3Xb1YvYqDG0ppL"
                }

                runOnUiThread { saveItemToDatabase(imageLink) }
            } catch (e: Exception) {
                Log.e("DriveUpload", "Error uploading image.", e)
                runOnUiThread { showToast("Failed to upload image.") }
            }
        }.start()
    }

    private fun saveItemToDatabase(imageUrl: String) {
        val productName = editName.text.toString()
        val unitsText = editUnits.text.toString().replace(" units", "")
        val units = unitsText.toIntOrNull()
        if (units == null) {
            showToast("Please enter a valid number for units.")
            return
        }

        val productCode = editCode.text.toString()
        val productCategory = editCategory.text.toString()
        val supplier = editSupplier.text.toString()

        val locationSpinner: Spinner = findViewById(R.id.editLocation)
        val selectedLocation = locationSpinner.selectedItem?.toString() ?: "No location selected"

        val dateAdded = editDateAdded.text.toString()
        val lastRestocked = if (units > originalStocksLeft) {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        } else {
            editLastRestocked.text.toString()
        }

        val currentProductName = intent.getStringExtra("productName") ?: ""

        firebaseDatabaseHelper.doesProductNameExistExcludingCurrent(productName, currentProductName) { exists ->
            if (exists) {
                showToast("Product name already exists. Please choose a different name.")
                return@doesProductNameExistExcludingCurrent
            }

            val item = Item(
                itemCode = productCode,
                itemName = productName,
                itemCategory = productCategory,
                location = selectedLocation,
                supplier = supplier,
                stocksLeft = units,
                dateAdded = dateAdded,
                lastRestocked = lastRestocked,
                imageUrl = imageUrl,
                enabled = true
            )

            firebaseDatabaseHelper.updateItem(productCode, item) { success ->
                if (success) {
                    val resultIntent = Intent().apply {
                        putExtra("updateStatus", true)
                        putExtra("updatedItem", item)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    showToast("Failed to update item.")
                }
            }
        }
    }


    private fun showCancelConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel, null)
        val yesButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val noButton = dialogView.findViewById<Button>(R.id.noBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        yesButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupSpinners() {
        val locations = arrayOf("Store Front", "Store Stock Room", "Porta Vaga Stock Room", "YMCA Stock Room", "Home")

        val locationSpinner: Spinner = findViewById(R.id.editLocation)
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = locationAdapter

        locationSpinner.setSelection(-1)

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                isLocationSelected = position >= 0
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupHeader() {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_back_arrow_circle)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

        val spannableString = SpannableString("  ${headerProduct.text}")
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

        headerProduct.text = spannableString
        headerProduct.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupImageChooser() {
        editImg.setOnClickListener {
            val intent = Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
            }
            imageChooserLauncher.launch(intent)
        }
    }

    private fun setupDatePickers() {
        editLastRestocked.setOnClickListener { showDatePicker(editLastRestocked) }
    }

    private fun showDatePicker(editText: EditText) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            editText.setText(formattedDate)

        }, year, month, day)

        datePickerDialog.show()
    }

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }
}
