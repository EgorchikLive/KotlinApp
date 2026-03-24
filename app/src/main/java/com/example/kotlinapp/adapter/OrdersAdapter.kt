package com.example.kotlinapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.databinding.ItemOrderBinding
import com.example.kotlinapp.models.Order
import java.text.SimpleDateFormat
import java.util.*

class OrdersAdapter(
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    private var orders: List<Order> = emptyList()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            try {
                binding.tvOrderId.text = "Заказ №${order.id.takeLast(8)}"
                binding.tvOrderDate.text = dateFormat.format(order.createdAt)
                binding.tvOrderTotal.text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}"

                val itemsCount = order.items.sumOf { it.quantity }
                binding.tvItemsCount.text = "${itemsCount} ${getItemsWord(itemsCount)}"

                binding.tvOrderStatus.text = getStatusText(order.status)
                binding.tvOrderStatus.setTextColor(getStatusColor(order.status))
                binding.tvOrderStatus.setBackgroundResource(getStatusBackground(order.status))

                binding.root.setOnClickListener {
                    try {
                        println("DEBUG: Click on order: ${order.id}")
                        onOrderClick(order)
                    } catch (e: Exception) {
                        println("DEBUG: Error in click: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Error binding order: ${e.message}")
                e.printStackTrace()
            }
        }

        private fun getStatusText(status: String): String {
            return when (status) {
                "pending" -> "Ожидает подтверждения"
                "confirmed" -> "Подтвержден"
                "shipped" -> "Отправлен"
                "delivered" -> "Доставлен"
                "cancelled" -> "Отменен"
                else -> status
            }
        }

        private fun getStatusColor(status: String): Int {
            return when (status) {
                "pending" -> android.graphics.Color.parseColor("#FF9800")
                "confirmed" -> android.graphics.Color.parseColor("#2196F3")
                "shipped" -> android.graphics.Color.parseColor("#9C27B0")
                "delivered" -> android.graphics.Color.parseColor("#4CAF50")
                "cancelled" -> android.graphics.Color.parseColor("#F44336")
                else -> android.graphics.Color.parseColor("#9E9E9E")
            }
        }

        private fun getStatusBackground(status: String): Int {
            return when (status) {
                "pending" -> R.drawable.status_pending_background
                "confirmed" -> R.drawable.status_confirmed_background
                "shipped" -> R.drawable.status_shipped_background
                "delivered" -> R.drawable.status_delivered_background
                "cancelled" -> R.drawable.status_cancelled_background
                else -> R.drawable.status_default_background
            }
        }

        private fun getItemsWord(count: Int): String {
            return when {
                count % 10 == 1 && count % 100 != 11 -> "товар"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "товара"
                else -> "товаров"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}