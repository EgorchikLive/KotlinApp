package com.example.kotlinapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kotlinapp.databinding.ActivityProductDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var cartRepository: CartRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private var currentProduct: Product? = null
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cartRepository = CartRepository()
        favoritesRepository = FavoritesRepository()

        setupToolbar()
        loadProductData()
        setupClickListeners()
        checkFavoriteStatus()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadProductData() {
        // Получаем данные из Intent
        currentProduct = Product(
            id = intent.getStringExtra("PRODUCT_ID") ?: "",
            name = intent.getStringExtra("PRODUCT_NAME") ?: "",
            price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0),
            imageUrl = intent.getStringExtra("PRODUCT_IMAGE") ?: "",
            description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "",
            category = intent.getStringExtra("PRODUCT_CATEGORY") ?: "",
            inStock = intent.getBooleanExtra("PRODUCT_IN_STOCK", true),
            rating = intent.getDoubleExtra("PRODUCT_RATING", 0.0)
        )

        currentProduct?.let { product ->
            // Устанавливаем данные
            binding.toolbar.title = product.name
            binding.productName.text = product.name
            binding.productPrice.text = "$${product.price}"
            binding.productDescription.text = product.description

            // Отображаем рейтинг
            displayRating(product.rating)

            // Отображаем статус наличия
            if (!product.inStock) {
                binding.stockStatus.visibility = android.view.View.VISIBLE
                binding.stockStatus.text = "Нет в наличии"
                binding.stockStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                binding.btnAddToCart.isEnabled = false
            } else {
                binding.stockStatus.visibility = android.view.View.GONE
                binding.btnAddToCart.isEnabled = true
            }

            // Загружаем изображение
            if (product.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(product.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error_image)
                    .into(binding.productImage)
            }
        }
    }

    private fun displayRating(rating: Double) {
        // Форматируем рейтинг до 1 знака после запятой
        val formattedRating = String.format("%.1f", rating)

        // Создаем строку с звездочками
        val stars = when {
            rating >= 4.5 -> "★★★★★"
            rating >= 4.0 -> "★★★★☆"
            rating >= 3.0 -> "★★★☆☆"
            rating >= 2.0 -> "★★☆☆☆"
            rating >= 1.0 -> "★☆☆☆☆"
            else -> "☆☆☆☆☆"
        }

        // Устанавливаем текст с рейтингом
        binding.productRating.text = "$stars $formattedRating"

        // Опционально: меняем цвет в зависимости от рейтинга
        when {
            rating >= 4.0 -> binding.productRating.setTextColor(getColor(android.R.color.holo_orange_dark))
            rating >= 3.0 -> binding.productRating.setTextColor(getColor(android.R.color.holo_blue_dark))
            rating >= 2.0 -> binding.productRating.setTextColor(getColor(android.R.color.holo_green_dark))
            else -> binding.productRating.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun checkFavoriteStatus() {
        currentProduct?.let { product ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val favorite = favoritesRepository.isProductInFavorites(product.id)
                    withContext(Dispatchers.Main) {
                        isFavorite = favorite
                        updateFavoriteIcon(favorite)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Кнопка избранного
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // Кнопка добавления в корзину
        binding.btnAddToCart.setOnClickListener {
            currentProduct?.let { product ->
                addToCart(product)
            }
        }
    }

    private fun toggleFavorite() {
        currentProduct?.let { product ->
            // Блокируем кнопку на время операции
            binding.btnFavorite.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val success = if (isFavorite) {
                        // Удаляем из избранного
                        favoritesRepository.removeFromFavorites(product.id)
                    } else {
                        // Добавляем в избранное
                        favoritesRepository.addToFavorites(product)
                    }

                    withContext(Dispatchers.Main) {
                        if (success) {
                            isFavorite = !isFavorite
                            updateFavoriteIcon(isFavorite)

                            val message = if (isFavorite) {
                                "Товар добавлен в избранное"
                            } else {
                                "Товар удален из избранного"
                            }
                            Toast.makeText(
                                this@ProductDetailActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ProductDetailActivity,
                                "Ошибка при обновлении избранного",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        binding.btnFavorite.isEnabled = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnFavorite.isEnabled = true
                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Ошибка: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            binding.btnFavorite.setColorFilter(getColor(android.R.color.holo_red_dark))
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            binding.btnFavorite.setColorFilter(null)
        }
    }

    private fun addToCart(product: Product) {
        if (!product.inStock) {
            Toast.makeText(
                this,
                "Товар отсутствует в наличии",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Блокируем кнопку и меняем текст
        binding.btnAddToCart.isEnabled = false
        binding.btnAddToCart.text = "Добавление..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = cartRepository.addToCart(product)

                withContext(Dispatchers.Main) {
                    binding.btnAddToCart.isEnabled = true
                    binding.btnAddToCart.text = "В корзину"

                    if (success) {
                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Товар добавлен в корзину",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Ошибка при добавлении в корзину",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnAddToCart.isEnabled = true
                    binding.btnAddToCart.text = "В корзину"
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}