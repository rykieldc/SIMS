package com.example.sims

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

@Suppress("DEPRECATION")
class RecyclerEditProductAdapter(
    private val getActivity: EditItemsActivityList,
    private var productList: MutableList<Product> = ArrayList()
) : RecyclerView.Adapter<RecyclerEditProductAdapter.ProductViewHolder>() {

    var originalList = productList.toMutableList()

    companion object {
        private const val REQUEST_CODE_EDIT_ITEM = 1001
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_img_placeholder)
            .error(R.drawable.ic_img_placeholder)
            .into(holder.productImg)

        holder.productSupplier.text = product.supplier
        holder.productName.text = product.itemName
        holder.productNum.text = product.stocksLeft

        holder.cardView.setOnClickListener {
            val intent =
                Intent(holder.itemView.context, EditItemActivity::class.java).apply {
                    putExtra("productImg", product.imageUrl)
                    putExtra("productName", product.itemName)
                    putExtra("productSupplier", product.supplier)
                    putExtra("productNum", product.stocksLeft)
                    putExtra("productCode", product.itemCode)
                    putExtra("productCategory", product.itemCategory)
                    putExtra("productWeight", product.itemWeight)
                    putExtra("productLocation", product.location)
                    putExtra("dateAdded", product.dateAdded)
                    putExtra("lastRestocked", product.lastRestocked)
                }
            getActivity.startActivityForResult(intent, REQUEST_CODE_EDIT_ITEM)
        }
    }

    fun filter(query: String) {
        val filteredList = originalList.filter { product ->
            product.itemName.lowercase().contains(query.lowercase())
        }
        updateList(filteredList)
    }

    fun resetList() {
        updateList(originalList)
    }

    fun filterByCategory(category: String): Int {
        val filteredList = originalList.filter { product ->
            product.itemCategory == category
        }
        updateList(filteredList)
        return filteredList.size
    }

    fun filterByLocation(location: String): Int {
        val filteredList = originalList.filter {
            it.location.equals(location, ignoreCase = true)
        }
        updateList(filteredList)
        return filteredList.size
    }


    private fun updateList(newList: List<Product>) {
        val diffCallback = ProductDiffCallback(productList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        productList.clear()
        productList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImg: ImageView = itemView.findViewById(R.id.product_img)
        val productSupplier: TextView = itemView.findViewById(R.id.product_supplier)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productNum: TextView = itemView.findViewById(R.id.product_num)
        val cardView: CardView = itemView.findViewById(R.id.productCardView)
    }

    class ProductDiffCallback(private val oldList: List<Product>, private val newList: List<Product>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].itemCode == newList[newItemPosition].itemCode
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}
