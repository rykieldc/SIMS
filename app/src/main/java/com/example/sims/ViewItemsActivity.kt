package com.example.sims

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewItemsActivity : AppCompatActivity() {

    private lateinit var header: TextView
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
        recyclerView = findViewById(R.id.rvViewItems) as RecyclerView
        recyclerViewProductAdapter = RecyclerViewProductAdapter(this@ViewItemsActivity, productList)

        val layoutManager: RecyclerView.LayoutManager = GridLayoutManager(this, 2)
        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.adapter = recyclerViewProductAdapter

        // Fetch items from the database
        fetchItemsFromDatabase()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Function to fetch items from Firebase
    private fun fetchItemsFromDatabase() {
        val databaseHelper = FirebaseDatabaseHelper()
        databaseHelper.fetchItems { itemsList ->
            productList.clear()
            productList.addAll(itemsList.map { item ->
                Product(item.supplier, item.itemName, "${item.stocksLeft} units", item.imageUrl)
            })
            recyclerViewProductAdapter?.notifyDataSetChanged()
        }
    }
}
