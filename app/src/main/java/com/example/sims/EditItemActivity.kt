package com.example.sims

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
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
import java.util.Calendar

class EditItemActivity : AppCompatActivity() {
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper
    private lateinit var headerProduct: TextView
    private lateinit var editImg: ImageView
    private lateinit var editName: EditText
    private lateinit var editUnits: EditText
    private lateinit var editCode: EditText
    private lateinit var editCategory: Spinner
    private lateinit var editLocation: Spinner
    private lateinit var editSupplier: EditText
    private lateinit var editDateAdded: EditText
    private lateinit var editLastRestocked: EditText
    private lateinit var imageChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var originalProductCode: String
    private lateinit var originalProductCategory: String


    private val calendar: Calendar = Calendar.getInstance()

    private var isCategorySelected = false
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
        editDateAdded.isEnabled = false

        imageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { imageUri ->
                    editImg.setImageURI(imageUri)
                }
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val categories = arrayOf(
            "Syringes & Needles", "Dressings & Bandages", "Disinfectants & Antiseptics",
            "Personal Protective Equipment (PPE)", "Diagnostic Devices", "Others"
        )
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
        editSupplier.setText(intent.getStringExtra("productSupplier"))
        editDateAdded.setText(intent.getStringExtra("dateAdded"))
        editLastRestocked.setText(intent.getStringExtra("lastRestocked"))

        intent.getStringExtra("productCategory")?.let { category ->
            val categoryPosition = categories.indexOf(category)
            if (categoryPosition != -1) {
                editCategory.setSelection(categoryPosition)
            }
        }

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

    private fun validateInputs(): Boolean {
        if (editImg.drawable == null) {
            showToast("Please select an image.")
            return false
        }

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

        if (!isCategorySelected) {
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
            saveItemToDatabase()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveItemToDatabase() {
        val imageUri = ""
        val productName = editName.text.toString()
        val unitsText = editUnits.text.toString().replace(" units", "")
        val units = unitsText.toIntOrNull()
        if (units == null) {
            showToast("Please enter a valid number for units.")
            return
        }

        val productCode = editCode.text.toString()
        val supplier = editSupplier.text.toString()

        val categorySpinner: Spinner = findViewById(R.id.editCategory)
        val selectedCategory = categorySpinner.selectedItem?.toString() ?: "No category selected"

        val locationSpinner: Spinner = findViewById(R.id.editLocation)
        val selectedLocation = locationSpinner.selectedItem?.toString() ?: "No location selected"

        val dateAdded = editDateAdded.text.toString()
        val lastRestocked = editLastRestocked.text.toString()

        val currentProductName = intent.getStringExtra("productName") ?: ""

        firebaseDatabaseHelper.doesProductNameExistExcludingCurrent(productName, currentProductName) { exists ->
            if (exists) {
                showToast("Product name already exists. Please choose a different name.")
                return@doesProductNameExistExcludingCurrent
            }

            val item = Item(
                itemCode = productCode,
                itemName = productName,
                itemCategory = selectedCategory,
                location = selectedLocation,
                supplier = supplier,
                stocksLeft = units,
                dateAdded = dateAdded,
                lastRestocked = lastRestocked,
                imageUrl = imageUri,
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
        val categories = arrayOf("Syringes & Needles", "Dressings & Bandages", "Disinfectants & Antiseptics", "Personal Protective Equipment (PPE)", "Diagnostic Devices", "Others")
        val locations = arrayOf("Store Front", "Store Stock Room", "Porta Vaga Stock Room", "YMCA Stock Room", "Home")

        val categorySpinner: Spinner = findViewById(R.id.editCategory)
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        categorySpinner.setSelection(-1)

        editCode.text.toString()


        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position >= 0) {
                    val selectedCategory = categories[position]
                    if (selectedCategory != originalProductCategory) {
                        fetchNextProductCode(selectedCategory)
                    } else {
                        editCode.setText(originalProductCode)
                    }
                    isCategorySelected = true
                } else {
                    isCategorySelected = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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

    private fun fetchNextProductCode(selectedCategory: String) {
        firebaseDatabaseHelper.getNextProductCode(selectedCategory) { newCode ->
            newCode?.let {
                editCode.setText(it)
                Log.d("EditItemActivity", "New product code set: $it")
            } ?: run {
                editCode.setText("")
                Log.d("EditItemActivity", "No product code found for category: $selectedCategory")
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
