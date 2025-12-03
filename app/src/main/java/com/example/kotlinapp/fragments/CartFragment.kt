package com.example.kotlinapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinapp.databinding.FragmentCartBinding
import kotlinx.coroutines.*

class CartFragment : SafeFragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartRepository: CartRepository
    private lateinit var cartAdapter: CartAdapter

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

        setupRecyclerView()
        loadCartItems()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Перезагружаем корзину при возвращении на фрагмент
        loadCartItems()
    }

    override fun onStop() {
        super.onStop()
        loadCartJob?.cancel()
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

    private fun loadCartItems() {
        loadCartJob?.cancel()

        loadCartJob = safeLaunch {
            try {
                if (!isAdded) return@safeLaunch
                binding.progressBar.visibility = View.VISIBLE

                val cartItems = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext emptyList<CartItem>()
                    cartRepository.getCartItems()
                }

                val total = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext 0.0
                    cartRepository.getCartTotal()
                }

                // Проверяем, что фрагмент еще отображается
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
                    binding.textTotalAmount.text = "$${String.format("%.2f", total)}"
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return@safeLaunch
                }

                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Ошибка загрузки корзины", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (isAdded && view != null) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCheckout.setOnClickListener {
            Toast.makeText(requireContext(), "Переход к оформлению заказа", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCartItemQuantity(cartItemId: String, newQuantity: Int) {
        safeLaunch {
            try {
                val success = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext false
                    cartRepository.updateCartItemQuantity(cartItemId, newQuantity)
                }

                if (isAdded && success) {
                    loadCartItems() // Перезагружаем данные
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