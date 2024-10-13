package com.example.sims

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewItemsActivity : AppCompatActivity() {

    private var recyclerView : RecyclerView? = null
    private var recyclerViewProductAdapter : RecyclerViewProductAdapter? = null
    private var productList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_items)

        productList = ArrayList()


        recyclerView = findViewById<View>(R.id.rvViewItems) as RecyclerView
        recyclerViewProductAdapter = RecyclerViewProductAdapter(this@ViewItemsActivity, productList)

        val layoutManager :RecyclerView.LayoutManager = GridLayoutManager(this, 2)
        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.adapter = recyclerViewProductAdapter

        prepareProductListData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun prepareProductListData(){
        var product = Product("QuickMed", "Syringes (5mL)", "500 units", R.drawable.sims_logo)
        productList.add(product)

        product = Product("Supplier", "Product Name", "# units", R.drawable.ic_upload_img)
        productList.add(product)
    }

}