package com.example.sims

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

@Suppress("DEPRECATION")
class ViewItemDetailsActivity : AppCompatActivity() {

    private lateinit var header: TextView
    private lateinit var itemImg: ImageView
    private lateinit var itemName: TextView
    private lateinit var itemUnits: TextView
    private lateinit var itemWeight: TextView
    private lateinit var itemCode: TextView
    private lateinit var itemCategory: TextView
    private lateinit var itemLocation: TextView
    private lateinit var itemSupplier: TextView
    private lateinit var itemDateAdded: TextView
    private lateinit var itemLastRestocked: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button

    companion object {
        private const val REQUEST_CODE_EDIT_ITEM = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_item_details)

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

        itemImg = findViewById(R.id.itemImg)
        itemName = findViewById(R.id.itemName)
        itemUnits = findViewById(R.id.itemUnits)
        itemCode = findViewById(R.id.itemCode)
        itemCategory = findViewById(R.id.itemCategory)
        itemWeight = findViewById(R.id.itemWeight)
        itemLocation = findViewById(R.id.itemLocation)
        itemSupplier = findViewById(R.id.itemSupplier)
        itemDateAdded = findViewById(R.id.itemDateAdded)
        itemLastRestocked = findViewById(R.id.itemLastRestocked)

        val productImgUrl = intent.getStringExtra("productImg")
        productImgUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_img_placeholder)
                .error(R.drawable.ic_img_placeholder)
                .into(itemImg)
        }
        itemName.text = intent.getStringExtra("productName")
        itemUnits.text = intent.getStringExtra("productNum") ?: "N/A"
        itemWeight.text = intent.getStringExtra("productWeight") ?: "N/A"
        itemCode.text = intent.getStringExtra("productCode")
        itemCategory.text = intent.getStringExtra("productCategory")
        itemLocation.text = intent.getStringExtra("productLocation")
        itemSupplier.text = intent.getStringExtra("productSupplier")
        itemDateAdded.text = intent.getStringExtra("dateAdded")
        itemLastRestocked.text = intent.getStringExtra("lastRestocked")

        editButton = findViewById(R.id.editBtn)
        editButton.setOnClickListener {
            val intent = Intent(this, EditItemActivity::class.java)
            intent.putExtra("productImg", productImgUrl)
            intent.putExtra("productName", itemName.text.toString())
            intent.putExtra("productNum", itemUnits.text.toString())
            intent.putExtra("productWeight", itemWeight.text.toString())
            intent.putExtra("productCode", itemCode.text.toString())
            intent.putExtra("productCategory", itemCategory.text.toString())
            intent.putExtra("productLocation", itemLocation.text.toString())
            intent.putExtra("productSupplier", itemSupplier.text.toString())
            intent.putExtra("dateAdded", itemDateAdded.text.toString())
            intent.putExtra("lastRestocked", itemLastRestocked.text.toString())
            startActivityForResult(intent, REQUEST_CODE_EDIT_ITEM)
        }

        deleteButton = findViewById(R.id.deleteBtn)
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(itemCode.text.toString())
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT_ITEM && resultCode == Activity.RESULT_OK) {
            val updateStatus = data?.getBooleanExtra("updateStatus", false) ?: false
            if (updateStatus) {
                val updatedItem: Item? = data?.getParcelableExtra("updatedItem")
                updatedItem?.let {
                    refreshItemDetails(it)
                }
                setResult(Activity.RESULT_OK, Intent().apply { putExtra("updateStatus", true) })
            }
        }
    }

    private fun refreshItemDetails(updatedItem: Item) {
        itemName.text = updatedItem.itemName
        "${updatedItem.stocksLeft} units".also { itemUnits.text = it }

        "${updatedItem.itemWeight} g".also { itemWeight.text = it }
        itemCode.text = updatedItem.itemCode
        itemCategory.text = updatedItem.itemCategory
        itemLocation.text = updatedItem.location
        itemSupplier.text = updatedItem.supplier
        itemDateAdded.text = updatedItem.dateAdded
        itemLastRestocked.text = updatedItem.lastRestocked

        Glide.with(this)
            .load(updatedItem.imageUrl)
            .placeholder(R.drawable.ic_img_placeholder)
            .error(R.drawable.ic_img_placeholder)
            .into(itemImg)
    }

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }

    private fun showDeleteConfirmationDialog(itemCode: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_item, null)
        val deleteButton = dialogView.findViewById<Button>(R.id.yesBtn)
        val cancelButton = dialogView.findViewById<Button>(R.id.noBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        deleteButton.setOnClickListener {
            val databaseHelper = FirebaseDatabaseHelper()
            databaseHelper.setItemEnabled(itemCode, false) { success ->
                if (success) {
                    Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK, Intent().apply { putExtra("updateStatus", true) })
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
