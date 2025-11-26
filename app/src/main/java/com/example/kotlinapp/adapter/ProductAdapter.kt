package com.example.kotlinapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.databinding.ItemProductBinding

class ProductAdapter(
    private var products: List<Product> = listOf(),
    private val onItemClick: (Product) -> Unit,
    private val onItemLongClick: (Product) -> Boolean = { false } // Добавили долгое нажатие
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productName.text = product.name
            binding.productPrice.text = "$${product.price}"
            binding.productImage.setImageResource(R.drawable.ic_placeholder)
            binding.productRating.text = "★ ${product.rating}"

            if (!product.inStock) {
                binding.outOfStockLabel.visibility = View.VISIBLE
            } else {
                binding.outOfStockLabel.visibility = View.GONE
            }

            // Обычный клик
            binding.root.setOnClickListener {
                onItemClick(product)
            }

            // Долгое нажатие
            binding.root.setOnLongClickListener {
                onItemLongClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}