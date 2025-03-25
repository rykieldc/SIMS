package com.example.sims

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.drive.model.Permission
import java.io.IOException
import java.util.Calendar
import java.io.File

class AddItemActivity : AppCompatActivity() {
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper
    private lateinit var headerProduct: TextView
    private lateinit var uploadImg: ImageView
    private lateinit var uploadDateAdded: EditText
    private lateinit var uploadWeight: EditText
    private lateinit var uploadLastRestocked: EditText
    private lateinit var productCodeEditText: EditText
    private lateinit var productNameEditText: EditText

    private lateinit var unitsEditText: EditText
    private lateinit var supplierEditText: EditText
    private lateinit var imageChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var driveService: Drive

    private val driveScopes = listOf("https://www.googleapis.com/auth/drive.file")
    private val calendar: Calendar = Calendar.getInstance()
    private var imageUri: Uri? = null
    private var isCategorySelected = false
    private var isLocationSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_item)

        headerProduct = findViewById(R.id.header)
        uploadImg = findViewById(R.id.uploadImg)
        uploadDateAdded = findViewById(R.id.uploadDateAdded)
        uploadWeight = findViewById(R.id.uploadWeight)
        uploadLastRestocked = findViewById(R.id.uploadLastRestocked)
        productCodeEditText = findViewById(R.id.uploadCode)
        productNameEditText = findViewById(R.id.uploadName)
        unitsEditText = findViewById(R.id.uploadUnits)
        supplierEditText = findViewById(R.id.uploadSupplier)
        firebaseDatabaseHelper = FirebaseDatabaseHelper()

        uploadLastRestocked.isEnabled = false
        productCodeEditText.isEnabled = false

        imageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                imageUri = data?.data
                uploadImg.setImageURI(imageUri)
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                uploadImg.setImageURI(imageUri)  // Display the captured image
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

        setupSpinners()
        setupHeader()
        setupImageChooser()
        setupDatePickers()
        setupGoogleSignIn()
        checkAndRequestPermissions()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun createImageFile(): File? {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(
                "JPEG_${System.currentTimeMillis()}",
                ".jpg",
                storageDir
            )
        } catch (ex: IOException) {
            null
        }
    }

    private val PERMISSION_REQUEST_CODE = 101

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
            false
        } else {
            openCamera()
            true
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
        if (productNameEditText.text.isNullOrEmpty()) {
            showToast("Please enter the product name.")
            return false
        }

        if (uploadWeight.text.isNullOrEmpty()) {
            showToast("Please enter the weight.")
            return false
        }

        if (unitsEditText.text.isNullOrEmpty()) {
            showToast("Please enter the units.")
            return false
        }

        if (supplierEditText.text.isNullOrEmpty()) {
            showToast("Please enter the supplier.")
            return false
        }

        if (productCodeEditText.text.isNullOrEmpty()) {
            showToast("Please enter the product code.")
            return false
        }

        if (!isCategorySelected) {
            showToast("Please select a category.")
            return false
        }

        if (!isLocationSelected) {
            showToast("Please select a location.")
            return false
        }

        if (uploadDateAdded.text.isNullOrEmpty()) {
            showToast("Please enter the date added.")
            return false
        }

        if (uploadLastRestocked.text.isNullOrEmpty()) {
            showToast("Please enter the last restocked date.")
            return false
        }

        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                    val metadata = DriveFile().apply {
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
                    // Use the default image URL if no image is uploaded
                    "https://drive.google.com/uc?id=1Y1RW22Vb4E02UMlMvjY1-qA1xlpPa0dc"
                }

                runOnUiThread { saveItemToDatabase(imageLink) }
            } catch (e: Exception) {
                Log.e("DriveUpload", "Error uploading image.", e)
                runOnUiThread { showToast("Failed to upload image.") }
            }
        }.start()
    }

    private fun saveItemToDatabase(imageUrl: String) {
        val productName = productNameEditText.text.toString()
        val units = unitsEditText.text.toString().toIntOrNull()
        val weight = uploadWeight.text.toString().toFloatOrNull()
        val productCode = productCodeEditText.text.toString()
        val supplier = supplierEditText.text.toString()
        val categorySpinner: Spinner = findViewById(R.id.uploadCategory)
        val selectedCategory = categorySpinner.selectedItem?.toString() ?: "No category selected"
        val locationSpinner: Spinner = findViewById(R.id.uploadLocation)
        val selectedLocation = locationSpinner.selectedItem?.toString() ?: "No location selected"
        val dateAdded = uploadDateAdded.text.toString()
        val lastRestocked = uploadLastRestocked.text.toString()

        firebaseDatabaseHelper.doesProductNameExist(productName) { exists ->
            if (exists) {
                showToast("Product name already exists.")
                return@doesProductNameExist
            }

            val item = Item(
                itemCode = productCode,
                itemName = productName,
                itemCategory = selectedCategory,
                itemWeight = (weight ?: 0).toFloat(),
                location = selectedLocation,
                supplier = supplier,
                stocksLeft = units ?: 0,
                dateAdded = dateAdded,
                lastRestocked = lastRestocked,
                imageUrl = imageUrl,
                enabled = true
            )

            firebaseDatabaseHelper.saveItem(item) { success ->

                if (success) {
                    showToast("Item saved successfully!")
                    finish()
                } else {
                    showToast("Failed to save item.")
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
        val categories = arrayOf("Select Category", "Syringes & Needles", "Dressings & Bandages", "Disinfectants & Antiseptics", "Personal Protective Equipment (PPE)", "Diagnostic Devices", "Others")
        val locations = arrayOf("Select Location", "Store Front", "Store Stock Room", "Porta Vaga Stock Room", "YMCA Stock Room", "Home")
        val categorySpinner: Spinner = findViewById(R.id.uploadCategory)
        val locationSpinner: Spinner = findViewById(R.id.uploadLocation)

        val categoryAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                if (position == 0) {
                    view.setTextColor(Color.GRAY)
                } else {
                    view.setTextColor(Color.BLACK)
                }
                return view
            }
        }

        val locationAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, locations) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                if (position == 0) {
                    view.setTextColor(Color.GRAY)
                } else {
                    view.setTextColor(Color.BLACK)
                }
                return view
            }
        }

        categorySpinner.adapter = categoryAdapter
        locationSpinner.adapter = locationAdapter

        categorySpinner.setSelection(0)
        locationSpinner.setSelection(0)

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    fetchNextProductCode(categories[position])
                    isCategorySelected = true
                } else {
                    isCategorySelected = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                isLocationSelected = position > 0
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                isLocationSelected = false
            }
        }
    }

    private fun fetchNextProductCode(selectedCategory: String) {
        firebaseDatabaseHelper.getNextProductCode(selectedCategory) { newCode ->
            newCode?.let {
                productCodeEditText.setText(it)
                Log.d("AddItemActivity", "New product code set: $it")
            } ?: run {
                productCodeEditText.setText("")
                Log.d("AddItemActivity", "No product code found for category: $selectedCategory")
            }
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
        uploadImg.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Image")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()  // Capture a new image
                    1 -> openGallery() // Pick an image from the gallery
                }
            }
            imageChooserLauncher.launch(intent)
            builder.show()
        }
    }

    private fun openCamera() {
        try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "JPEG_${System.currentTimeMillis()}.jpg")

            if (photoFile == null) {
                Log.e("CameraError", "Failed to create image file")
                showToast("Error: Unable to create image file.")
                return
            }

            val photoURI = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            imageUri = photoURI

            Log.d("CameraDebug", "Photo URI: $imageUri")

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            cameraLauncher.launch(cameraIntent)

        } catch (e: Exception) {
            Log.e("CameraError", "Error launching camera", e)
            showToast("Error: Failed to open camera.")
        }
    }

    private fun openGallery() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        imageChooserLauncher.launch(intent)
    }

    private fun setupDatePickers() {
        uploadDateAdded.setOnClickListener { showDatePicker(uploadDateAdded) }
    }

    private fun showDatePicker(editText: EditText) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            editText.setText(formattedDate)

            if (editText == uploadDateAdded) {
                uploadLastRestocked.isEnabled = true
                uploadLastRestocked.setText(formattedDate)
            } else {
                val addedDate = uploadDateAdded.text.toString()
                if (!isLastRestockedValid(addedDate, formattedDate)) {
                    uploadLastRestocked.setText(addedDate)
                }
            }
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun isLastRestockedValid(addedDate: String, restockedDate: String): Boolean {
        val addedParts = addedDate.split("/")
        val restockedParts = restockedDate.split("/")

        val addedCalendar = Calendar.getInstance().apply {
            set(addedParts[2].toInt(), addedParts[1].toInt() - 1, addedParts[0].toInt())
        }
        val restockedCalendar = Calendar.getInstance().apply {
            set(restockedParts[2].toInt(), restockedParts[1].toInt() - 1, restockedParts[0].toInt())
        }

        return !restockedCalendar.before(addedCalendar)
    }

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }
}