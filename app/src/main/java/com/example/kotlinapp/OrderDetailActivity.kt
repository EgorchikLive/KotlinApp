package com.example.kotlinapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinapp.adapter.OrderItemsAdapter
import com.example.kotlinapp.databinding.ActivityOrderDetailBinding
import com.example.kotlinapp.databinding.CustomAppbarProfileBinding
import com.example.kotlinapp.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private lateinit var appBarBinding: CustomAppbarProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var orderItemsAdapter: OrderItemsAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var orderId: String = ""
    private var order: Order? = null
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Скрываем системный ActionBar
        supportActionBar?.hide()

        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем binding для кастомного AppBar
        appBarBinding = CustomAppbarProfileBinding.bind(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupAppBar()
        setupRecyclerView()
        loadOrderData()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun setupAppBar() {
        // Скрываем иконку профиля
        appBarBinding.profileIcon.visibility = View.GONE
        // Устанавливаем заголовок
        appBarBinding.toolbarTitle.text = "Детали заказа"
        // Настраиваем кнопку назад
        appBarBinding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }


    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemsAdapter()
        binding.recyclerViewOrderItems.apply {
            layoutManager = LinearLayoutManager(this@OrderDetailActivity)
            adapter = orderItemsAdapter
        }
    }

    private fun loadOrderData() {
        orderId = intent.getStringExtra("ORDER_ID") ?: ""
        val orderTotal = intent.getDoubleExtra("ORDER_TOTAL", 0.0)
        val orderStatus = intent.getStringExtra("ORDER_STATUS") ?: "pending"
        val orderDate = intent.getLongExtra("ORDER_DATE", 0)

        if (orderId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID заказа не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        scope.launch {
            try {
                val orderDoc = withContext(Dispatchers.IO) {
                    db.collection("orders").document(orderId).get().await()
                }

                withContext(Dispatchers.Main) {
                    if (orderDoc.exists()) {
                        order = orderDoc.toObject(Order::class.java)
                        order?.let {
                            displayOrderDetails(it)
                        }
                    } else {
                        displayOrderFromIntent(orderId, orderTotal, orderStatus, orderDate)
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    displayOrderFromIntent(orderId, orderTotal, orderStatus, orderDate)
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.visibility = View.VISIBLE
                    Toast.makeText(
                        this@OrderDetailActivity,
                        "Загружены основные данные заказа",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun displayOrderFromIntent(orderId: String, total: Double, status: String, date: Long) {
        binding.tvOrderId.text = "Заказ №${orderId.takeLast(8)}"
        binding.tvOrderDate.text = if (date > 0) {
            dateFormat.format(Date(date))
        } else {
            "Дата неизвестна"
        }
        binding.tvOrderStatus.text = getStatusText(status)
        binding.tvOrderStatus.setTextColor(getStatusColor(status))

        binding.tvSubtotal.text = "$${String.format(Locale.US, "%.2f", total)}"
        binding.tvDeliveryFee.text = "$0.00"
        binding.tvTotal.text = "$${String.format(Locale.US, "%.2f", total)}"

        binding.tvPaymentMethod.text = "Оплата при получении"

        binding.tvEmptyItems.visibility = View.VISIBLE
        binding.recyclerViewOrderItems.visibility = View.GONE
        binding.tvShippingAddress.text = "Адрес не указан"
    }

    private fun displayOrderDetails(order: Order) {
        binding.tvOrderId.text = "Заказ №${order.id.takeLast(8)}"
        binding.tvOrderDate.text = dateFormat.format(order.createdAt)
        binding.tvOrderStatus.text = getStatusText(order.status)
        binding.tvOrderStatus.setTextColor(getStatusColor(order.status))

        order.shippingAddress?.let { address ->
            val addressText = buildString {
                append(address.fullName)
                append("\n${address.addressLine1}")
                if (address.addressLine2.isNotEmpty()) {
                    append(", ${address.addressLine2}")
                }
                append("\n${address.city}, ${address.state} ${address.postalCode}")
                append("\n${address.country}")
                append("\nТел: ${address.phoneNumber}")
            }
            binding.tvShippingAddress.text = addressText
        } ?: run {
            binding.tvShippingAddress.text = "Адрес не указан"
        }

        if (order.items.isNotEmpty()) {
            orderItemsAdapter.updateItems(order.items)
            binding.recyclerViewOrderItems.visibility = View.VISIBLE
            binding.tvEmptyItems.visibility = View.GONE
        } else {
            binding.recyclerViewOrderItems.visibility = View.GONE
            binding.tvEmptyItems.visibility = View.VISIBLE
        }

        binding.tvSubtotal.text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}"
        binding.tvDeliveryFee.text = "$0.00"
        binding.tvTotal.text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}"
        binding.tvPaymentMethod.text = "Оплата при получении"
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

    private fun setupClickListeners() {
        binding.btnTrackOrder.setOnClickListener {
            Toast.makeText(this, "Отслеживание заказа", Toast.LENGTH_SHORT).show()
        }

        binding.btnContactSupport.setOnClickListener {
            Toast.makeText(this, "Служба поддержки", Toast.LENGTH_SHORT).show()
        }

        binding.btnRepeatOrder.setOnClickListener {
            repeatOrder()
        }
    }

    private fun repeatOrder() {
        order?.let { order ->
            scope.launch {
                try {
                    val cartRepository = CartRepository()
                    var successCount = 0

                    for (item in order.items) {
                        val product = Product(
                            id = item.productId,
                            name = item.productName,
                            price = item.productPrice,
                            imageUrl = item.productImageUrl
                        )
                        val success = withContext(Dispatchers.IO) {
                            cartRepository.addToCart(product)
                        }
                        if (success) successCount++
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@OrderDetailActivity,
                            "$successCount товаров добавлено в корзину",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@OrderDetailActivity,
                            "Ошибка при добавлении в корзину",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}