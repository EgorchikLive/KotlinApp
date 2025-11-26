package com.example.kotlinapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kotlinapp.databinding.ActivityProductDetailBinding

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "Товар"
        val productPrice = intent.getDoubleExtra("PRODUCT_PRICE", 0.0)
        val productImage = intent.getStringExtra("PRODUCT_IMAGE") ?: ""
        val productDescription = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: ""

        // Устанавливаем данные
        binding.toolbar.title = productName
        binding.productName.text = productName
        binding.productPrice.text = "$$productPrice"
        binding.productDescription.text = productDescription

        // Загружаем изображение
        if (productImage.isNotEmpty()) {
            Glide.with(this)
                .load(productImage)
                .placeholder(R.drawable.ic_placeholder)
                .into(binding.productImage)
        }
    }

    private fun setupClickListeners() {
        binding.btnAddToCart.setOnClickListener {
            Toast.makeText(this, "Товар добавлен в корзину", Toast.LENGTH_SHORT).show()
        }

        binding.btnBuyNow.setOnClickListener {
            Toast.makeText(this, "Переход к оформлению заказа", Toast.LENGTH_SHORT).show()
        }
    }
}