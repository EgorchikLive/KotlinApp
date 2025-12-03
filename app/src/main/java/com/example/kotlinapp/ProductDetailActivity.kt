package com.example.kotlinapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kotlinapp.databinding.ActivityProductDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var cartRepository: CartRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cartRepository = CartRepository()

        setupToolbar()
        loadProductData()
        setupClickListeners()
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
        val product = Product(
            id = intent.getStringExtra("PRODUCT_ID") ?: "",
            name = intent.getStringExtra("PRODUCT_NAME") ?: "",
            price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0),
            imageUrl = intent.getStringExtra("PRODUCT_IMAGE") ?: "",
            description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "",
            category = intent.getStringExtra("PRODUCT_CATEGORY") ?: "",
            inStock = intent.getBooleanExtra("PRODUCT_IN_STOCK", true),
            rating = intent.getDoubleExtra("PRODUCT_RATING", 0.0)
        )

        // Устанавливаем данные
        binding.toolbar.title = product.name
        binding.productName.text = product.name
        binding.productPrice.text = "$${product.price}"
        binding.productDescription.text = product.description

        // Загружаем изображение
        if (product.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(binding.productImage)
        }
    }

    private fun setupClickListeners() {
        binding.btnAddToCart.setOnClickListener {
            val product = Product(
                id = intent.getStringExtra("PRODUCT_ID") ?: "",
                name = intent.getStringExtra("PRODUCT_NAME") ?: "",
                price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0),
                imageUrl = intent.getStringExtra("PRODUCT_IMAGE") ?: "",
                description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "",
                category = intent.getStringExtra("PRODUCT_CATEGORY") ?: "",
                inStock = intent.getBooleanExtra("PRODUCT_IN_STOCK", true),
                rating = intent.getDoubleExtra("PRODUCT_RATING", 0.0)
            )

            addToCart(product)
        }

        binding.btnBuyNow.setOnClickListener {
            val product = Product(
                id = intent.getStringExtra("PRODUCT_ID") ?: "",
                name = intent.getStringExtra("PRODUCT_NAME") ?: "",
                price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0),
                imageUrl = intent.getStringExtra("PRODUCT_IMAGE") ?: "",
                description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "",
                category = intent.getStringExtra("PRODUCT_CATEGORY") ?: "",
                inStock = intent.getBooleanExtra("PRODUCT_IN_STOCK", true),
                rating = intent.getDoubleExtra("PRODUCT_RATING", 0.0)
            )

            CoroutineScope(Dispatchers.IO).launch {
                cartRepository.addToCart(product)

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Товар добавлен в корзину. Переход к оформлению заказа",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun addToCart(product: Product) {
        CoroutineScope(Dispatchers.IO).launch {
            val success = cartRepository.addToCart(product)

            CoroutineScope(Dispatchers.Main).launch {
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
        }
    }
}