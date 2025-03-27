package com.example.sims

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditItemsActivityList : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_VIEW_ITEM_DETAILS = 1001
    }

    private lateinit var header: TextView
    private lateinit var searchView: SearchView
    private lateinit var filterSpinner: Spinner
    private lateinit var filterIcon: ImageView
    private lateinit var noProductsText: TextView
    private var selectedCategory: String? = null
    private var recyclerView: RecyclerView? = null
    private var recyclerViewProductAdapter: RecyclerEditProductAdapter? = null
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
            EditItemsActivityList.DrawableClickSpan { onBackPressedDispatcher.onBackPressed() },
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        filterSpinner = findViewById(R.id.filterBtn)
        filterIcon = findViewById(R.id.filterIcon)

        filterSpinner.visibility = View.INVISIBLE

        filterIcon.setOnClickListener {
            filterSpinner.performClick()
        }


        val filterChoices = listOf(
            "None",
            "Syringes & Needles",
            "Dressings & Bandages",
            "Disinfectants & Antiseptics",
            "Personal Protective Equipment (PPE)",
            "Diagnostic Devices",
            "Other Items"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterChoices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedCategory = filterChoices[position]
                applyCategoryFilter(selectedCategory!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        noProductsText = findViewById(R.id.noProductsText)

        header.text = spannableString
        header.movementMethod = LinkMovementMethod.getInstance()

        productList = ArrayList()
        recyclerView = findViewById(R.id.rvViewItems)!!
        recyclerViewProductAdapter = RecyclerEditProductAdapter(this@EditItemsActivityList, productList)

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
                    selectedCategory?.let {
                        applyCategoryFilter(it)
                    } ?: recyclerViewProductAdapter?.resetList()
                } else {
                    recyclerViewProductAdapter?.filter(newText)
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

    class DrawableClickSpan(private val clickListener: () -> Unit) : ClickableSpan() {
        override fun onClick(widget: View) {
            clickListener()
        }
    }

    private fun applyCategoryFilter(category: String) {
        if (category == "None") {
            recyclerViewProductAdapter?.resetList()
            noProductsText.visibility = View.GONE
        } else {
            recyclerViewProductAdapter?.filterByCategory(category)
            if (recyclerViewProductAdapter?.itemCount == 0) {
                noProductsText.text = getString(R.string.no_products_found, category)
                noProductsText.visibility = View.VISIBLE
            } else {
                noProductsText.visibility = View.GONE
            }
        }
    }



    override fun onResume() {
        super.onResume()
        fetchItemsFromDatabase()
        searchView.setQuery("", false)
        searchView.clearFocus()
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
        val localDatabase = AppDatabase.getDatabase(this).itemDao()

        if (!isInternetAvailable()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val cachedItems = localDatabase.getAllItems()
                withContext(Dispatchers.Main) {
                    if (cachedItems.isNotEmpty()) {
                        updateProductList(cachedItems.map { it.toProduct() })
                    } else {
                        noProductsText.text = "No products available offline."
                        noProductsText.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            databaseHelper.fetchItems { itemsList ->
                val products = itemsList.map { item ->
                    Product(
                        itemCode = item.itemCode,
                        itemName = item.itemName,
                        itemCategory = item.itemCategory,
                        itemWeight = "${item.itemWeight} g",
                        location = item.location,
                        supplier = item.supplier,
                        stocksLeft = "${item.stocksLeft} unit(s)",
                        dateAdded = item.dateAdded,
                        lastRestocked = item.lastRestocked,
                        imageUrl = item.imageUrl
                    )
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    localDatabase.insertAll(products.map { it.toProductEntity() })
                }

                updateProductList(products)
            }
        }
    }

    fun LocalItem.toProduct(): Product {
        return Product(
            itemCode = this.itemCode,
            itemName = this.itemName,
            itemCategory = this.itemCategory,
            itemWeight = "${this.itemWeight} g",
            location = this.location,
            supplier = this.supplier,
            stocksLeft = "${this.stocksLeft} unit(s)",
            dateAdded = this.dateAdded,
            lastRestocked = this.lastRestocked,
            imageUrl = this.imageUrl
        )
    }


    fun Product.toProductEntity(): LocalItem {
        return LocalItem(
            itemCode = this.itemCode,
            itemName = this.itemName,
            itemCategory = this.itemCategory,
            itemWeight = this.itemWeight.replace(" g", "").toFloatOrNull() ?: 0f,
            location = this.location,
            supplier = this.supplier,
            stocksLeft = this.stocksLeft.replace(" unit(s)", "").toIntOrNull() ?: 0,
            dateAdded = this.dateAdded,
            lastRestocked = this.lastRestocked,
            enabled = true,
            imageUrl = this.imageUrl
        )
    }


    private fun updateProductList(products: List<Product>) {
        productList.clear()
        productList.addAll(products)

        recyclerViewProductAdapter?.apply {
            originalList.clear()
            originalList.addAll(productList)
            notifyDataSetChanged()
        }

        noProductsText.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

}
