package com.example.sims

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class AddItemActivity : AppCompatActivity() {
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper
    private lateinit var headerProduct: TextView
    private lateinit var uploadImg: ImageView
    private lateinit var uploadDateAdded: EditText
    private lateinit var uploadLastRestocked: EditText
    private lateinit var productCodeEditText: EditText
    private lateinit var productNameEditText: EditText
    private lateinit var unitsEditText: EditText
    private lateinit var supplierEditText: EditText // New field for supplier
    private lateinit var imageChooserLauncher: ActivityResultLauncher<Intent>

    private val calendar: Calendar = Calendar.getInstance()

    // Track selections for spinners
    private var isCategorySelected = false
    private var isLocationSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_item)

        headerProduct = findViewById(R.id.header)
        uploadImg = findViewById(R.id.uploadImg)
        uploadDateAdded = findViewById(R.id.uploadDateAdded)
        uploadLastRestocked = findViewById(R.id.uploadLastRestocked)
        productCodeEditText = findViewById(R.id.uploadCode)
        productNameEditText = findViewById(R.id.uploadName)
        unitsEditText = findViewById(R.id.uploadUnits)
        supplierEditText = findViewById(R.id.uploadSupplier) // Initialize supplier field
        firebaseDatabaseHelper = FirebaseDatabaseHelper()

        uploadLastRestocked.isEnabled = false
        productCodeEditText.isEnabled = false

        imageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { imageUri ->
                    uploadImg.setImageURI(imageUri)
                }
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // New method to validate input fields
    private fun validateInputs(): Boolean {
        // Check if image is set
        if (uploadImg.drawable == null) {
            uploadImg.setImageURI(null) // Set image value to an empty string
        }

        // Check if product name is filled
        if (productNameEditText.text.isNullOrEmpty()) {
            showToast("Please enter the product name.")
            return false
        }

        // Check if units are filled
        if (unitsEditText.text.isNullOrEmpty()) {
            showToast("Please enter the units.")
            return false
        }

        // Check if supplier is filled
        if (supplierEditText.text.isNullOrEmpty()) {
            showToast("Please enter the supplier.")
            return false
        }

        // Check if product code is filled
        if (productCodeEditText.text.isNullOrEmpty()) {
            showToast("Please enter the product code.")
            return false
        }

        // Check if category is selected
        if (!isCategorySelected) {
            showToast("Please select a category.")
            return false
        }

        // Check if location is selected
        if (!isLocationSelected) {
            showToast("Please select a location.")
            return false
        }

        // Check if date added is filled
        if (uploadDateAdded.text.isNullOrEmpty()) {
            showToast("Please enter the date added.")
            return false
        }

        // Check if last restocked date is filled
        if (uploadLastRestocked.text.isNullOrEmpty()) {
            showToast("Please enter the last restocked date.")
            return false
        }

        return true // All validations passed
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
            saveItemToDatabase() // Call the new save function
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveItemToDatabase() {
        val imageUri = "" // Set image URL to empty string for now

        // Get product details
        val productName = productNameEditText.text.toString()
        val units = unitsEditText.text.toString().toIntOrNull() // Use toIntOrNull to avoid crashes
        val productCode = productCodeEditText.text.toString()
        val supplier = supplierEditText.text.toString() // Get supplier input

        // Get selected category and location
        val categorySpinner: Spinner = findViewById(R.id.uploadCategory)
        val selectedCategory = categorySpinner.selectedItem?.toString() ?: "No category selected"

        val locationSpinner: Spinner = findViewById(R.id.uploadLocation)
        val selectedLocation = locationSpinner.selectedItem?.toString() ?: "No location selected"

        // Get dates
        val dateAdded = uploadDateAdded.text.toString()
        val lastRestocked = uploadLastRestocked.text.toString()

        // Check if the product name already exists
        firebaseDatabaseHelper.doesProductNameExist(productName) { exists ->
            if (exists) {
                showToast("Product name already exists. Please choose a different name.")
                return@doesProductNameExist // Exit the lambda to prevent saving
            }

            // Create Item object to save
            val item = Item(
                itemCode = productCode,
                itemName = productName,
                itemCategory = selectedCategory,
                location = selectedLocation,
                supplier = supplier,
                stocksLeft = units ?: 0, // Default to 0 if units is null
                dateAdded = dateAdded,
                lastRestocked = lastRestocked,
                imageUrl = imageUri // Image URL is currently empty
            )

            // Save item to database
            firebaseDatabaseHelper.saveItem(item) { success ->
                if (success) {
                    showToast("Item saved successfully!")
                    finish() // Close the activity after saving
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
            finish() // Close the activity
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

        // Custom adapter for categories
        val categoryAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0 // Disable the first item
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                if (position == 0) {
                    view.setTextColor(Color.GRAY) // Set hint text color to gray
                } else {
                    view.setTextColor(Color.BLACK) // Set other items color to black
                }
                return view
            }
        }

        // Custom adapter for locations
        val locationAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, locations) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0 // Disable the first item
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                if (position == 0) {
                    view.setTextColor(Color.GRAY) // Set hint text color to gray
                } else {
                    view.setTextColor(Color.BLACK) // Set other items color to black
                }
                return view
            }
        }

        categorySpinner.adapter = categoryAdapter
        locationSpinner.adapter = locationAdapter

        // Set the first item as selected initially
        categorySpinner.setSelection(0) // Select the placeholder
        locationSpinner.setSelection(0) // Select the placeholder

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) { // Ignore the first position, which is the placeholder
                    fetchNextProductCode(categories[position]) // Fetch the product code for the selected category
                    isCategorySelected = true // Mark category as selected
                } else {
                    isCategorySelected = false // Reset flag if placeholder is selected
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                isLocationSelected = position > 0 // Mark location as selected if not placeholder
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
                Log.d("AddItemActivity", "New product code set: $it") // Log the new product code
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
            val intent = Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
            }
            imageChooserLauncher.launch(intent)
        }
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