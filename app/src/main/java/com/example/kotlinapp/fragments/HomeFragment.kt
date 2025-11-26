package com.example.kotlinapp

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kotlinapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var productAdapter: ProductAdapter
    private lateinit var productRepository: ProductRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Включаем меню в фрагменте
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        productRepository = ProductRepository()

        checkUserRole()
        setupRecyclerView()
        loadProducts()
    }

    private fun checkUserRole() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userDoc = db.collection("users").document(user.uid).get().await()
                    val userData = userDoc.toObject(User::class.java)

                    CoroutineScope(Dispatchers.Main).launch {
                        isAdmin = userData?.role == "admin"
                        activity?.invalidateOptionsMenu() // Обновляем меню

                        if (isAdmin) {
                            binding.fabAddProduct.visibility = View.VISIBLE
                        } else {
                            binding.fabAddProduct.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        isAdmin = false
                        binding.fabAddProduct.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.productsRecyclerView.layoutManager = gridLayoutManager

        // Добавляем кастомные отступы
        binding.productsRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val spacing = 1.dpToPx(requireContext())
                outRect.left = spacing
                outRect.right = spacing
                outRect.top = spacing
                outRect.bottom = spacing
            }
        })

        // Создаем адаптер с поддержкой долгого нажатия для админа
        productAdapter = ProductAdapter(emptyList(),
            onItemClick = { product ->
                openProductDetail(product)
            },
            onItemLongClick = { product ->
                if (isAdmin) {
                    showProductActionsDialog(product)
                }
                true
            }
        )

        binding.productsRecyclerView.adapter = productAdapter

        // Кнопка добавления товара (только для админа)
        binding.fabAddProduct.setOnClickListener {
            if (isAdmin) {
                openAddProductActivity()
            }
        }
    }

    private fun loadProducts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val products = productRepository.getAllProducts()

                CoroutineScope(Dispatchers.Main).launch {
                    if (products.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.productsRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.productsRecyclerView.visibility = View.VISIBLE
                        productAdapter.updateProducts(products)
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Ошибка загрузки товаров", Toast.LENGTH_SHORT).show()
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
            putExtra("IS_ADMIN", isAdmin)
        }
        startActivity(intent)
    }

    private fun showProductActionsDialog(product: Product) {
        val options = arrayOf("Редактировать", "Удалить")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Действия с товаром")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openEditProductActivity(product)
                    1 -> deleteProduct(product)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openAddProductActivity() {
        val intent = Intent(requireContext(), EditProductActivity::class.java)
        startActivity(intent)
    }

    private fun openEditProductActivity(product: Product) {
        val intent = Intent(requireContext(), EditProductActivity::class.java).apply {
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

    private fun deleteProduct(product: Product) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Удаление товара")
            .setMessage("Вы уверены, что хотите удалить \"${product.name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val success = productRepository.deleteProduct(product.id)

                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            Toast.makeText(requireContext(), "Товар удален", Toast.LENGTH_SHORT).show()
                            loadProducts() // Перезагружаем список
                        } else {
                            Toast.makeText(requireContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Функция для массовой загрузки тестовых данных
    private fun bulkLoadSampleProducts() {
        val sampleProducts = listOf(
            Product(
                name = "Смартфон Samsung Galaxy S23",
                price = 799.99,
                imageUrl = "https://via.placeholder.com/300",
                description = "Флагманский смартфон с лучшей камерой",
                category = "Электроника",
                inStock = true,
                rating = 4.5
            ),
            Product(
                name = "Ноутбук MacBook Pro",
                price = 1299.99,
                imageUrl = "https://via.placeholder.com/300",
                description = "Мощный ноутбук для работы и творчества",
                category = "Электроника",
                inStock = true,
                rating = 4.8
            ),
            Product(
                name = "Беспроводные наушники",
                price = 199.99,
                imageUrl = "https://via.placeholder.com/300",
                description = "Качественный звук и шумоподавление",
                category = "Аксессуары",
                inStock = false,
                rating = 4.3
            ),
            Product(
                name = "Умные часы Apple Watch",
                price = 399.99,
                imageUrl = "https://via.placeholder.com/300",
                description = "Следите за здоровьем и получайте уведомления",
                category = "Гаджеты",
                inStock = true,
                rating = 4.6
            ),
            Product(
                name = "Игровая консоль PlayStation 5",
                price = 499.99,
                imageUrl = "https://via.placeholder.com/300",
                description = "Новейшая игровая консоль от Sony",
                category = "Игры",
                inStock = true,
                rating = 4.9
            ),
            Product(
                name = "Фотокамера Canon EOS R5",
                price = 3899.99,
                imageUrl = "https://via.placeholder.com/300",
                description = "Профессиональная зеркальная камера",
                category = "Фототехника",
                inStock = true,
                rating = 4.7
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            val success = productRepository.bulkAddProducts(sampleProducts)

            CoroutineScope(Dispatchers.Main).launch {
                if (success) {
                    Toast.makeText(requireContext(), "Товары загружены в Firestore", Toast.LENGTH_SHORT).show()
                    loadProducts() // Перезагружаем список
                } else {
                    Toast.makeText(requireContext(), "Ошибка загрузки товаров", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)

        // Показываем пункт меню только для админа
        val bulkLoadItem = menu.findItem(R.id.action_bulk_load)
        bulkLoadItem.isVisible = isAdmin

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_bulk_load -> {
                if (isAdmin) {
                    bulkLoadSampleProducts()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Extension function для конвертации dp в px
    private fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}