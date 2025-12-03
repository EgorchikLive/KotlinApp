package com.example.kotlinapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kotlinapp.databinding.FragmentFavoritesBinding
import kotlinx.coroutines.*

class FavoritesFragment : SafeFragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var productAdapter: ProductAdapter

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
        setupRecyclerView()
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    override fun onStop() {
        super.onStop()
        loadFavoritesJob?.cancel()
    }

    private fun setupRecyclerView() {
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewFavorites.layoutManager = gridLayoutManager

        productAdapter = ProductAdapter(
            emptyList(),
            onItemClick = { product ->
                openProductDetail(product)
            },
            onFavoriteClick = { product, isFavorite ->
                handleFavoriteClick(product, isFavorite)
            }
        )

        binding.recyclerViewFavorites.adapter = productAdapter
    }

    private fun loadFavorites() {
        loadFavoritesJob?.cancel()

        loadFavoritesJob = safeLaunch {
            try {
                if (!isAdded) return@safeLaunch
                binding.progressBar.visibility = View.VISIBLE

                val favoriteProducts = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext emptyList<Product>()
                    favoritesRepository.getFavoriteProducts()
                }

                if (!isAdded || view == null) return@safeLaunch

                if (favoriteProducts.isEmpty()) {
                    binding.textNoFavorites.visibility = View.VISIBLE
                    binding.recyclerViewFavorites.visibility = View.GONE
                } else {
                    binding.textNoFavorites.visibility = View.GONE
                    binding.recyclerViewFavorites.visibility = View.VISIBLE
                    productAdapter.updateProducts(favoriteProducts)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return@safeLaunch
                }

                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Ошибка загрузки избранного", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (isAdded && view != null) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun handleFavoriteClick(product: Product, isFavorite: Boolean) {
        safeLaunch {
            try {
                val success = withContext(Dispatchers.IO) {
                    if (!isActive) return@withContext false
                    if (isFavorite) {
                        favoritesRepository.addToFavorites(product)
                    } else {
                        favoritesRepository.removeFromFavorites(product.id)
                    }
                }

                if (isAdded && success) {
                    val message = if (isFavorite) {
                        "Товар добавлен в избранное"
                    } else {
                        "Товар удален из избранного"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                    if (!isFavorite) {
                        loadFavorites()
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException && isAdded) {
                    Toast.makeText(requireContext(), "Ошибка", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openProductDetail(product: Product) {
        // Реализуйте открытие деталей товара
    }
}