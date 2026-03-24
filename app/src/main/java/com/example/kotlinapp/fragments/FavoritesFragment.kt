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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.kotlinapp.databinding.FragmentFavoritesBinding
import kotlinx.coroutines.*

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var favoritesRepository: FavoritesRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadFavoritesJob: Job? = null

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

        setupSwipeRefresh()
        setupRecyclerView()
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        favoritesAdapter.cancelAllJobs()
        loadFavoritesJob?.cancel()
        scope.cancel()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на фрагмент
        loadFavorites()
    }

    override fun onStop() {
        super.onStop()
        loadFavoritesJob?.cancel()
        // Останавливаем анимацию обновления если она идет
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun setupSwipeRefresh() {
        // Настройка цветов индикатора обновления
        binding.swipeRefreshLayout.setColorSchemeColors(
            android.R.color.holo_blue_dark,
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_red_dark
        )

        // Устанавливаем слушатель обновления
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshFavorites()
        }
    }

    private fun refreshFavorites() {
        // Отменяем предыдущую загрузку
        loadFavoritesJob?.cancel()

        // Загружаем избранное заново
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
        // Отменяем предыдущую загрузку
        loadFavoritesJob?.cancel()

        loadFavoritesJob = scope.launch {
            try {
                // Показываем прогресс, если это не обновление свайпом
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                // Скрываем все элементы во время загрузки
                binding.textNoFavorites.visibility = View.GONE
                binding.recyclerViewFavorites.visibility = View.GONE

                val favoriteProducts = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext emptyList<Product>()
                    favoritesRepository.getFavoriteProducts()
                }

                // Проверяем, что фрагмент еще отображается
                if (!isAdded || view == null) return@launch

                if (favoriteProducts.isEmpty()) {
                    // Нет избранных товаров - показываем текст
                    binding.textNoFavorites.visibility = View.VISIBLE
                    binding.textNoFavorites.text = "У вас пока нет избранных товаров"
                    binding.recyclerViewFavorites.visibility = View.GONE
                } else {
                    // Есть товары - показываем RecyclerView
                    binding.textNoFavorites.visibility = View.GONE
                    binding.recyclerViewFavorites.visibility = View.VISIBLE
                    favoritesAdapter.updateProducts(favoriteProducts)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return@launch
                }

                if (isAdded && view != null) {
                    binding.textNoFavorites.visibility = View.VISIBLE
                    binding.textNoFavorites.text = "Ошибка загрузки: ${e.message}"
                    binding.recyclerViewFavorites.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Ошибка загрузки избранного",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                if (isAdded && view != null) {
                    binding.progressBar.visibility = View.GONE
                    // Останавливаем анимацию обновления
                    binding.swipeRefreshLayout.isRefreshing = false
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
                        if (!isActive) return@withContext false
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
                    if (e is CancellationException) return@launch

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
}