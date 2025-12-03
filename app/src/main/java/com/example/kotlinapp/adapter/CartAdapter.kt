package com.example.kotlinapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlinapp.databinding.ItemCartBinding

class CartAdapter(
    private var cartItems: List<CartItem> = emptyList(),
    private val onQuantityChanged: (String, Int) -> Unit,
    private val onItemRemoved: (String) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            binding.textProductName.text = cartItem.productName
            binding.textProductPrice.text = "$${cartItem.productPrice}"
            binding.textQuantity.text = cartItem.quantity.toString()
            binding.textItemTotal.text = "$${String.format("%.2f", cartItem.productPrice * cartItem.quantity)}"

            // Загрузка изображения
            if (cartItem.productImageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(cartItem.productImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(binding.imageProduct)
            }

            // Обработчики изменения количества
            binding.btnIncrease.setOnClickListener {
                val newQuantity = cartItem.quantity + 1
                onQuantityChanged(cartItem.id, newQuantity)
            }

            binding.btnDecrease.setOnClickListener {
                if (cartItem.quantity > 1) {
                    val newQuantity = cartItem.quantity - 1
                    onQuantityChanged(cartItem.id, newQuantity)
                }
            }

            // Удаление товара
            binding.btnRemove.setOnClickListener {
                onItemRemoved(cartItem.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateItems(newItems: List<CartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }
}