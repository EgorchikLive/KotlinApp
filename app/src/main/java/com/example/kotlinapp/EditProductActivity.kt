package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinapp.databinding.ActivityEditProductBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            // Очищаем поля при добавлении нового товара
            binding.productName.setText("")
            binding.productPrice.setText("")
            binding.productDescription.setText("")
            binding.productCategory.setText("")
            binding.switchInStock.isChecked = true
            binding.productRating.setText("0.0")
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

        // Валидация
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название товара", Toast.LENGTH_SHORT).show()
            return
        }

        if (priceText.isEmpty()) {
            Toast.makeText(this, "Введите цену товара", Toast.LENGTH_SHORT).show()
            return
        }

        val price = try {
            priceText.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Некорректная цена", Toast.LENGTH_SHORT).show()
            return
        }

        val rating = try {
            ratingText.toDouble().coerceIn(0.0, 5.0)
        } catch (e: NumberFormatException) {
            0.0
        }

        // Создаем продукт (без ID для новых товаров)
        val product = Product(
            id = if (isEditMode) productId else "", // Пустой ID для новых товаров
            name = name,
            price = price,
            imageUrl = "https://via.placeholder.com/300", // В реальном приложении загрузка изображения
            description = description,
            category = category,
            inStock = inStock,
            rating = rating
        )

        // Показываем индикатор загрузки
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Сохранение..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = if (isEditMode) {
                    // Обновляем существующий товар
                    val success = productRepository.updateProduct(product)
                    if (success) product else null
                } else {
                    // Создаем новый товар и получаем его с ID
                    productRepository.addProduct(product)
                }

                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = if (isEditMode) "Сохранить" else "Добавить"

                    if (result != null) {
                        val message = if (isEditMode) "Товар обновлен" else "Товар добавлен"
                        Toast.makeText(
                            this@EditProductActivity,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()

                        // Возвращаем результат
                        val intent = Intent().apply {
                            putExtra("PRODUCT_ID", result.id)
                            putExtra("PRODUCT_NAME", result.name)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@EditProductActivity,
                            "Ошибка сохранения",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = if (isEditMode) "Сохранить" else "Добавить"
                    Toast.makeText(
                        this@EditProductActivity,
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}