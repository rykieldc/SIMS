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
import com.google.firebase.database.*

class ViewItemDetailsActivity : AppCompatActivity() {

    private lateinit var header: TextView
    private lateinit var itemImg: ImageView
    private lateinit var itemName: TextView
    private lateinit var itemUnits: TextView
    private lateinit var itemWeight: TextView
    private lateinit var rackNo: TextView
    private lateinit var itemCode: TextView
    private lateinit var itemCategory: TextView
    private lateinit var itemLocation: TextView
    private lateinit var itemSupplier: TextView
    private lateinit var itemDateAdded: TextView
    private lateinit var itemLastRestocked: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button

    private lateinit var databaseReference: DatabaseReference
    private var itemCodeValue: String? = null
    private var itemListener: ValueEventListener? = null

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
        rackNo = findViewById(R.id.rackNo)
        itemLocation = findViewById(R.id.itemLocation)
        itemSupplier = findViewById(R.id.itemSupplier)
        itemDateAdded = findViewById(R.id.itemDateAdded)
        itemLastRestocked = findViewById(R.id.itemLastRestocked)

        itemCodeValue = intent.getStringExtra("productCode")

        databaseReference = FirebaseDatabase.getInstance().getReference("items")

        itemCodeValue?.let { code ->
            fetchItemDetails(code)
            listenForRealTimeUpdates(code)
        }

        editButton = findViewById(R.id.editBtn)

        deleteButton = findViewById(R.id.deleteBtn)
        deleteButton.setOnClickListener {
            itemCodeValue?.let { code -> showDeleteConfirmationDialog(code) }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchItemDetails(itemCode: String) {
        FirebaseDatabaseHelper().getItemByCode(itemCode) { item ->
            if (item != null) {
                refreshItemDetails(item)
            } else {
                Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listenForRealTimeUpdates(itemCode: String) {
        itemListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val updatedItem = child.getValue(Item::class.java)
                    updatedItem?.let { refreshItemDetails(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewItemDetailsActivity, "Failed to load updates", Toast.LENGTH_SHORT).show()
            }
        }

        databaseReference.orderByChild("itemCode").equalTo(itemCode)
            .addValueEventListener(itemListener!!)
    }

    private fun refreshItemDetails(updatedItem: Item) {
        itemName.text = updatedItem.itemName
        itemUnits.text = "${updatedItem.stocksLeft} units"
        itemWeight.text = "${updatedItem.itemWeight} g"
        rackNo.text = "${updatedItem.rackNo}"
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

        editButton.setOnClickListener {
            val intent = Intent(this, EditItemActivity::class.java).apply {
                putExtra("productCode", updatedItem.itemCode)
                putExtra("productName", updatedItem.itemName)
                putExtra("productNum", "${updatedItem.stocksLeft} unit(s)")
                putExtra("productWeight", "${updatedItem.itemWeight} g")
                putExtra("productRack", "${updatedItem.rackNo}")
                putExtra("productCategory", updatedItem.itemCategory)
                putExtra("productLocation", updatedItem.location)
                putExtra("productSupplier", updatedItem.supplier)
                putExtra("dateAdded", updatedItem.dateAdded)
                putExtra("lastRestocked", updatedItem.lastRestocked)
                putExtra("productImg", updatedItem.imageUrl)
            }
            startActivityForResult(intent, REQUEST_CODE_EDIT_ITEM)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        itemListener?.let {
            databaseReference.removeEventListener(it)
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
            FirebaseDatabaseHelper().setItemEnabled(itemCode, false) { success ->
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

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }
}
