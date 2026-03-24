package com.example.kotlinapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlinapp.CartItem
import com.example.kotlinapp.R
import com.example.kotlinapp.databinding.ItemOrderProductBinding
import java.util.Locale

class OrderItemsAdapter : RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder>() {

    private var items: List<CartItem> = emptyList()

    inner class OrderItemViewHolder(private val binding: ItemOrderProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvProductName.text = item.productName
            binding.tvProductPrice.text = "$${String.format(Locale.US, "%.2f", item.productPrice)}"
            binding.tvQuantity.text = "x${item.quantity}"
            binding.tvTotal.text = "$${String.format(Locale.US, "%.2f", item.productPrice * item.quantity)}"

            // Загрузка изображения
            if (item.productImageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.productImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error_image)
                    .into(binding.productImage)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val binding = ItemOrderProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}