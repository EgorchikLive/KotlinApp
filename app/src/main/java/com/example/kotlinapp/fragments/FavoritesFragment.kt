package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.databinding.FragmentFavoritesBinding
import kotlinx.coroutines.*

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var favoritesRepository: FavoritesRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favoritesRepository = FavoritesRepository()

        setupRecyclerView()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onItemClick = { product ->
                // Переход к деталям товара
                openProductDetail(product)
            },
            onFavoriteClick = { product, isFavorite ->
                // Обработка удаления из избранного
                toggleFavorite(product, isFavorite)
            }
        )

        binding.recyclerViewFavorites.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = favoritesAdapter
        }
    }

    private fun loadFavorites() {
        // Показываем ProgressBar, скрываем RecyclerView и текст
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewFavorites.visibility = View.GONE
        binding.textNoFavorites.visibility = View.GONE

        scope.launch {
            try {
                val favoriteProducts = withContext(Dispatchers.IO) {
                    favoritesRepository.getFavoriteProducts()
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE

                    if (favoriteProducts.isEmpty()) {
                        // Нет избранных товаров - показываем текст
                        binding.textNoFavorites.visibility = View.VISIBLE
                        binding.recyclerViewFavorites.visibility = View.GONE
                    } else {
                        // Есть товары - показываем RecyclerView
                        binding.textNoFavorites.visibility = View.GONE
                        binding.recyclerViewFavorites.visibility = View.VISIBLE
                        favoritesAdapter.updateProducts(favoriteProducts)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.textNoFavorites.visibility = View.VISIBLE
                    binding.textNoFavorites.text = "Ошибка загрузки: ${e.message}"
                    Toast.makeText(
                        requireContext(),
                        "Ошибка загрузки избранного",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun toggleFavorite(product: Product, isFavorite: Boolean) {
        // isFavorite = true - добавляем, false - удаляем
        // В избранном обычно только удаляем
        if (!isFavorite) {
            scope.launch {
                try {
                    val success = withContext(Dispatchers.IO) {
                        favoritesRepository.removeFromFavorites(product.id)
                    }

                    withContext(Dispatchers.Main) {
                        if (success) {
                            // Обновляем список после удаления
                            loadFavorites()
                            Toast.makeText(
                                requireContext(),
                                "Товар удален из избранного",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Ошибка при удалении",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun openProductDetail(product: Product) {
        val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_ID", product.id)
            putExtra("PRODUCT_NAME", product.name)
            putExtra("PRODUCT_PRICE", product.price)
            putExtra("PRODUCT_IMAGE", product.imageUrl)
            putExtra("PRODUCT_DESCRIPTION", product.description)
            putExtra("PRODUCT_CATEGORY", product.category)
            putExtra("PRODUCT_IN_STOCK", product.inStock)
            putExtra("PRODUCT_RATING", product.rating)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на фрагмент
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favoritesAdapter.cancelAllJobs()
        scope.cancel()
        _binding = null
    }
}