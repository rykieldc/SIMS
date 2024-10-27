package com.example.sims

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecyclerViewProductAdapter(
    private val getActivity: ViewItemsActivity,
    private val productList: List<Product>
) : RecyclerView.Adapter<RecyclerViewProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // Log the image URL for debugging
        Log.d("RecyclerViewProductAdapter", "Loading image from URL: ${product.imageUrl}")

        // Load the image from URL using Glide
        Glide.with(holder.itemView.context)
            .load(product.imageUrl) // Assuming imageUrl is a property of Product
            .placeholder(R.drawable.ic_img_placeholder) // Optional: placeholder image
            .error(R.drawable.ic_img_placeholder) // Optional: error image if loading fails
            .into(holder.productImg)

        holder.productSupplier.text = product.supplier
        holder.productName.text = product.name
        holder.productNum.text = product.units

        holder.cardView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ViewItemDetailsActivity::class.java).apply {
                putExtra("productImg", product.imageUrl) // Send the image URL to the detail activity
                putExtra("productName", product.name)
                putExtra("productSupplier", product.supplier)
                putExtra("productNum", product.units)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImg: ImageView = itemView.findViewById(R.id.product_img)
        val productSupplier: TextView = itemView.findViewById(R.id.product_supplier)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productNum: TextView = itemView.findViewById(R.id.product_num)
        val cardView: CardView = itemView.findViewById(R.id.productCardView)
    }
}
