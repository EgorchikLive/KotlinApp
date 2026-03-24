package com.example.kotlinapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinapp.*
import com.example.kotlinapp.databinding.FragmentCartBinding
import com.example.kotlinapp.models.Order
import com.example.kotlinapp.models.ShippingAddress
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class CartFragment : SafeFragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartRepository: CartRepository
    private lateinit var cartAdapter: CartAdapter
    private var shippingAddress: ShippingAddress? = null

    private var loadCartJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cartRepository = CartRepository()

        setupSwipeRefresh()
        setupRecyclerView()
        loadCartItems()
        setupClickListeners()
        loadShippingAddress()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        loadCartItems()
        loadShippingAddress()
    }

    override fun onStop() {
        super.onStop()
        loadCartJob?.cancel()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshCart()
        }
    }

    private fun refreshCart() {
        loadCartJob?.cancel()
        loadCartItems()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { cartItemId, newQuantity ->
                updateCartItemQuantity(cartItemId, newQuantity)
            },
            onItemRemoved = { cartItemId ->
                removeFromCart(cartItemId)
            }
        )

        binding.recyclerViewCart.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun loadShippingAddress() {
        safeLaunch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@safeLaunch

                val addressDoc = withContext(Dispatchers.IO) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(userId).collection("addresses")
                        .document("default").get().await()
                }

                if (addressDoc.exists()) {
                    shippingAddress = addressDoc.toObject(ShippingAddress::class.java)
                } else {
                    shippingAddress = null
                }
            } catch (e: Exception) {
                shippingAddress = null
            }
        }
    }

    private fun loadCartItems() {
        loadCartJob?.cancel()

        loadCartJob = safeLaunch {
            try {
                if (!isAdded) return@safeLaunch

                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                binding.textEmptyCart.visibility = View.GONE
                binding.recyclerViewCart.visibility = View.GONE
                binding.layoutCartTotal.visibility = View.GONE
                binding.btnCheckout.visibility = View.GONE

                val cartItems = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext emptyList<CartItem>()
                    cartRepository.getCartItems()
                }

                val total = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext 0.0
                    cartRepository.getCartTotal()
                }

                if (!isAdded || view == null) return@safeLaunch

                if (cartItems.isEmpty()) {
                    binding.textEmptyCart.visibility = View.VISIBLE
                    binding.recyclerViewCart.visibility = View.GONE
                    binding.layoutCartTotal.visibility = View.GONE
                    binding.btnCheckout.visibility = View.GONE
                } else {
                    binding.textEmptyCart.visibility = View.GONE
                    binding.recyclerViewCart.visibility = View.VISIBLE
                    binding.layoutCartTotal.visibility = View.VISIBLE
                    binding.btnCheckout.visibility = View.VISIBLE

                    cartAdapter.updateItems(cartItems)
                    val totalFormatted = String.format(Locale.US, "%.2f", total)
                    binding.textTotalAmount.text = "$$totalFormatted"
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@safeLaunch

                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Ошибка загрузки корзины", Toast.LENGTH_SHORT).show()
                    binding.textEmptyCart.visibility = View.VISIBLE
                    binding.recyclerViewCart.visibility = View.GONE
                    binding.layoutCartTotal.visibility = View.GONE
                    binding.btnCheckout.visibility = View.GONE
                }
            } finally {
                if (isAdded && view != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCheckout.setOnClickListener {
            proceedToCheckout()
        }
    }

    private fun proceedToCheckout() {
        if (shippingAddress == null) {
            showAddAddressDialog()
            return
        }

        val address = shippingAddress!!
        if (address.fullName.isEmpty() || address.addressLine1.isEmpty() ||
            address.city.isEmpty() || address.phoneNumber.isEmpty()) {
            showIncompleteAddressDialog()
            return
        }

        showOrderConfirmationDialog()
    }

    private fun showAddAddressDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Адрес доставки")
            .setMessage("Для оформления заказа необходимо указать адрес доставки. Хотите добавить адрес сейчас?")
            .setPositiveButton("Добавить") { _, _ ->
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showIncompleteAddressDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Неполный адрес")
            .setMessage("Ваш адрес доставки заполнен не полностью. Пожалуйста, укажите полный адрес в профиле.")
            .setPositiveButton("Перейти в профиль") { _, _ ->
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showOrderConfirmationDialog() {
        safeLaunch {
            val total = cartRepository.getCartTotal()
            val formattedTotal = String.format(Locale.US, "%.2f", total)

            AlertDialog.Builder(requireContext())
                .setTitle("Подтверждение заказа")
                .setMessage("""
                    Вы оформляете заказ на сумму $$formattedTotal.
                    
                    Адрес доставки:
                    ${shippingAddress?.fullName}
                    ${shippingAddress?.addressLine1}
                    ${shippingAddress?.city}, ${shippingAddress?.postalCode}
                    ${shippingAddress?.country}
                    Тел: ${shippingAddress?.phoneNumber}
                    
                    Подтвердить заказ?
                """.trimIndent())
                .setPositiveButton("Оформить") { _, _ ->
                    createOrder()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun createOrder() {
        binding.btnCheckout.isEnabled = false
        binding.btnCheckout.text = "Оформление..."

        safeLaunch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Необходимо войти в систему", Toast.LENGTH_SHORT).show()
                        binding.btnCheckout.isEnabled = true
                        binding.btnCheckout.text = "Оформить заказ"
                    }
                    return@safeLaunch
                }

                val cartItems = withContext(Dispatchers.IO) {
                    cartRepository.getCartItems()
                }

                if (cartItems.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Корзина пуста", Toast.LENGTH_SHORT).show()
                        binding.btnCheckout.isEnabled = true
                        binding.btnCheckout.text = "Оформить заказ"
                    }
                    return@safeLaunch
                }

                // Используем CartItem напрямую, не конвертируя в OrderItem
                val total = cartItems.sumOf { it.productPrice * it.quantity }

                val db = FirebaseFirestore.getInstance()
                val orderId = db.collection("orders").document().id

                val currentDate = Date()

                // Создаем заказ с cartItems как List<CartItem>
                val order = Order(
                    id = orderId,
                    userId = userId,
                    items = cartItems,
                    totalAmount = total,
                    status = "pending",
                    shippingAddress = shippingAddress,
                    createdAt = currentDate,
                    updatedAt = currentDate
                )

                withContext(Dispatchers.IO) {
                    db.collection("orders").document(orderId).set(order).await()

                    db.collection("users").document(userId).collection("orders")
                        .document(orderId).set(order).await()

                    cartRepository.clearCart()
                }

                withContext(Dispatchers.Main) {
                    binding.btnCheckout.isEnabled = true
                    binding.btnCheckout.text = "Оформить заказ"

                    Toast.makeText(requireContext(), "Заказ успешно оформлен!", Toast.LENGTH_LONG).show()
                    loadCartItems()
                    showOrderSuccessDialog(orderId)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnCheckout.isEnabled = true
                    binding.btnCheckout.text = "Оформить заказ"
                    Toast.makeText(requireContext(), "Ошибка оформления заказа: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showOrderSuccessDialog(orderId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Заказ оформлен!")
            .setMessage("""
                Ваш заказ №${orderId.takeLast(8)} успешно оформлен.
                
                Вы можете отслеживать статус заказа в разделе "Мои заказы" в профиле.
                
                Спасибо за покупку!
            """.trimIndent())
            .setPositiveButton("Хорошо") { _, _ ->
                // Закрываем диалог
            }
            .setNeutralButton("Мои заказы") { _, _ ->
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
            }
            .show()
    }

    private fun updateCartItemQuantity(cartItemId: String, newQuantity: Int) {
        safeLaunch {
            try {
                val success = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext false
                    cartRepository.updateCartItemQuantity(cartItemId, newQuantity)
                }

                if (isAdded && success) {
                    loadCartItems()
                } else if (isAdded) {
                    Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e !is CancellationException && isAdded) {
                    Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun removeFromCart(cartItemId: String) {
        safeLaunch {
            try {
                val success = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext false
                    cartRepository.removeFromCart(cartItemId)
                }

                if (isAdded && success) {
                    loadCartItems()
                    Toast.makeText(requireContext(), "Товар удален из корзины", Toast.LENGTH_SHORT).show()
                } else if (isAdded) {
                    Toast.makeText(requireContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e !is CancellationException && isAdded) {
                    Toast.makeText(requireContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}