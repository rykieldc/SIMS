package com.example.sims

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewProductAdapter(
    private val getActivity: ViewItemsActivity,
    private val productList: List<Product>) :
    RecyclerView.Adapter<RecyclerViewProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_product_item, parent, false)

        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.productImg.setImageResource(productList[position].img)
        holder.productSupplier.text = productList[position].supplier
        holder.productName.text = productList[position].name
        holder.productNum.text = productList[position].units

        holder.cardView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ViewItemDetailsActivity::class.java)
            intent.putExtra("productImg", productList[position].img)
            holder.itemView.context.startActivity(intent)
        }
    }


    class ProductViewHolder (itemView : View) : RecyclerView.ViewHolder(itemView) {
        val productImg : ImageView = itemView.findViewById(R.id.product_img)
        val productSupplier : TextView = itemView.findViewById(R.id.product_supplier)
        val productName : TextView = itemView.findViewById(R.id.product_name)
        val productNum : TextView = itemView.findViewById(R.id.product_num)
        val cardView : CardView = itemView.findViewById(R.id.productCardView)
    }

}