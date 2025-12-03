package com.example.kotlinapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.databinding.ItemProductBinding
import kotlinx.coroutines.*

class ProductAdapter(
    private var products: List<Product> = listOf(),
    private val onItemClick: (Product) -> Unit,
    private val onItemLongClick: (Product) -> Boolean = { false },
    private val onFavoriteClick: (Product, Boolean) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val favoritesRepository = FavoritesRepository()
    private val favoriteStatusMap = mutableMapOf<String, Boolean>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        // Отменяем все корутины при уничтожении адаптера
        CoroutineScope(Dispatchers.Main).launch {
            // Предзагружаем статусы избранного для первых N товаров
            products.take(10).forEach { product ->
                loadFavoriteStatusForProduct(product.id)
            }
        }
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var favoriteJob: Job? = null

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

            // Загружаем статус избранного
            loadFavoriteStatus(product.id)

            // Обработчик кнопки избранного
            binding.btnFavorite.setOnClickListener {
                val isFavorite = favoriteStatusMap[product.id] ?: false
                onFavoriteClick(product, !isFavorite)
            }

            binding.root.setOnClickListener {
                onItemClick(product)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(product)
            }
        }

        private fun loadFavoriteStatus(productId: String) {
            // Отменяем предыдущий запрос
            favoriteJob?.cancel()

            favoriteJob = scope.launch {
                try {
                    val isFavorite = withContext(Dispatchers.IO) {
                        favoritesRepository.isProductInFavorites(productId)
                    }
                    favoriteStatusMap[productId] = isFavorite
                    updateFavoriteIcon(isFavorite)
                } catch (e: Exception) {
                    // Игнорируем ошибки при загрузке статуса
                }
            }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            if (isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }

    private fun loadFavoriteStatusForProduct(productId: String) {
        scope.launch {
            try {
                val isFavorite = withContext(Dispatchers.IO) {
                    favoritesRepository.isProductInFavorites(productId)
                }
                favoriteStatusMap[productId] = isFavorite
            } catch (e: Exception) {
                // Игнорируем ошибки
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
        job.cancelChildren() // Отменяем все текущие загрузки
        products = newProducts
        favoriteStatusMap.clear()
        notifyDataSetChanged()
    }

    fun updateFavoriteStatus(productId: String, isFavorite: Boolean) {
        favoriteStatusMap[productId] = isFavorite
        val position = products.indexOfFirst { it.id == productId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun cancelAllJobs() {
        job.cancel()
    }
}