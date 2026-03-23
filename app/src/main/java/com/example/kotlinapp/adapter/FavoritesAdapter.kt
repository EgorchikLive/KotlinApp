package com.example.kotlinapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.kotlinapp.databinding.ItemProductBinding
import kotlinx.coroutines.*

class FavoritesAdapter(
    private var products: List<Product> = listOf(),
    private val onItemClick: (Product) -> Unit,
    private val onFavoriteClick: (Product, Boolean) -> Unit,
    private val onItemLongClick: (Product) -> Boolean = { false }
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    private val favoritesRepository = FavoritesRepository()
    private val favoriteStatusMap = mutableMapOf<String, Boolean>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        // Загружаем статусы избранного
        CoroutineScope(Dispatchers.Main).launch {
            products.forEach { product ->
                loadFavoriteStatusForProduct(product.id)
            }
        }
    }

    inner class FavoriteViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var favoriteJob: Job? = null

        fun bind(product: Product) {
            binding.productName.text = product.name
            binding.productPrice.text = "$${product.price}"
            binding.productRating.text = "★ ${product.rating}"

            if (!product.inStock) {
                binding.outOfStockLabel.visibility = View.VISIBLE
            } else {
                binding.outOfStockLabel.visibility = View.GONE
            }

            // Загрузка изображения
            loadProductImage(product.imageUrl)

            // Загружаем статус избранного
            loadFavoriteStatus(product.id)

            // Обработчик кнопки избранного
            binding.btnFavorite.setOnClickListener {
                val isFavorite = favoriteStatusMap[product.id] ?: true
                onFavoriteClick(product, !isFavorite)
            }

            // Обработчик клика на весь item
            binding.root.setOnClickListener {
                onItemClick(product)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(product)
            }
        }

        private fun loadProductImage(imageUrl: String) {
            if (imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error_image)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(8)))
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.ic_placeholder)
            }
        }

        private fun loadFavoriteStatus(productId: String) {
            favoriteJob?.cancel()

            favoriteJob = scope.launch {
                try {
                    val isFavorite = withContext(Dispatchers.IO) {
                        favoritesRepository.isProductInFavorites(productId)
                    }
                    favoriteStatusMap[productId] = isFavorite
                    updateFavoriteIcon(isFavorite)
                } catch (e: Exception) {
                    // Если ошибка, считаем что товар в избранном
                    favoriteStatusMap[productId] = true
                    updateFavoriteIcon(true)
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
                favoriteStatusMap[productId] = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        job.cancelChildren()
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