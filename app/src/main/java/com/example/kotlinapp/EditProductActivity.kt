package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.kotlinapp.databinding.ActivityEditProductBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProductBinding
    private lateinit var productRepository: ProductRepository
    private var isEditMode = false
    private var productId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productRepository = ProductRepository()

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
        // Проверяем, редактируем ли существующий товар
        productId = intent.getStringExtra("PRODUCT_ID") ?: ""
        isEditMode = productId.isNotEmpty()

        if (isEditMode) {
            binding.toolbar.title = "Редактировать товар"
            binding.productName.setText(intent.getStringExtra("PRODUCT_NAME") ?: "")
            binding.productPrice.setText(intent.getDoubleExtra("PRODUCT_PRICE", 0.0).toString())
            binding.productDescription.setText(intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "")
            binding.productCategory.setText(intent.getStringExtra("PRODUCT_CATEGORY") ?: "")
            binding.switchInStock.isChecked = intent.getBooleanExtra("PRODUCT_IN_STOCK", true)
            binding.productRating.setText(intent.getDoubleExtra("PRODUCT_RATING", 0.0).toString())
        } else {
            binding.toolbar.title = "Добавить товар"
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveProduct()
        }
    }

    private fun saveProduct() {
        val name = binding.productName.text.toString().trim()
        val priceText = binding.productPrice.text.toString().trim()
        val description = binding.productDescription.text.toString().trim()
        val category = binding.productCategory.text.toString().trim()
        val inStock = binding.switchInStock.isChecked
        val ratingText = binding.productRating.text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val price = try {
            priceText.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Некорректная цена", Toast.LENGTH_SHORT).show()
            return
        }

        val rating = try {
            ratingText.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }

        val product = Product(
            id = productId,
            name = name,
            price = price,
            imageUrl = "https://via.placeholder.com/300", // В реальном приложении загрузка изображения
            description = description,
            category = category,
            inStock = inStock,
            rating = rating
        )

        CoroutineScope(Dispatchers.IO).launch {
            val success = productRepository.addProduct(product)

            CoroutineScope(Dispatchers.Main).launch {
                if (success) {
                    Toast.makeText(
                        this@EditProductActivity,
                        if (isEditMode) "Товар обновлен" else "Товар добавлен",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProductActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}