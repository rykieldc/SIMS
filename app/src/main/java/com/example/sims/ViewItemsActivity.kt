package com.example.sims

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewItemsActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_VIEW_ITEM_DETAILS = 1001
    }

    private lateinit var header: TextView
    private lateinit var searchView: SearchView
    private var recyclerView: RecyclerView? = null
    private var recyclerViewProductAdapter: RecyclerViewProductAdapter? = null
    private var productList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_items)

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
            ViewItemDetailsActivity.DrawableClickSpan { onBackPressedDispatcher.onBackPressed() },
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        header.text = spannableString
        header.movementMethod = LinkMovementMethod.getInstance()

        productList = ArrayList()
        recyclerView = findViewById(R.id.rvViewItems)!!
        recyclerViewProductAdapter = RecyclerViewProductAdapter(this@ViewItemsActivity, productList)

        val layoutManager: RecyclerView.LayoutManager = GridLayoutManager(this, 2)
        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.adapter = recyclerViewProductAdapter

        fetchItemsFromDatabase()


        searchView = findViewById(R.id.searchProduct)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    recyclerViewProductAdapter?.resetList() // Reset list when query is empty
                } else {
                    recyclerViewProductAdapter?.filter(newText) // Filter list when query is not empty
                }
                return true
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        fetchItemsFromDatabase()
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_VIEW_ITEM_DETAILS && resultCode == Activity.RESULT_OK) {
            fetchItemsFromDatabase()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchItemsFromDatabase() {
        val databaseHelper = FirebaseDatabaseHelper()
        databaseHelper.fetchItems { itemsList ->
            productList.clear()
            productList.addAll(itemsList.map { item ->
                Product(
                    itemCode = item.itemCode,
                    itemName = item.itemName,
                    itemCategory = item.itemCategory,
                    location = item.location,
                    supplier = item.supplier,
                    stocksLeft = "${item.stocksLeft} units",
                    dateAdded = item.dateAdded,
                    lastRestocked = item.lastRestocked,
                    imageUrl = item.imageUrl
                )
            })

            recyclerViewProductAdapter?.apply {
                originalList.clear()
                originalList.addAll(productList)
                notifyDataSetChanged()
            }
        }
    }
}
