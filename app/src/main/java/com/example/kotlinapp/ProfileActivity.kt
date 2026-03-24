package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kotlinapp.databinding.ActivityProfileBinding
import com.example.kotlinapp.databinding.CustomAppbarProfileBinding
import com.example.kotlinapp.models.Order
import com.example.kotlinapp.models.ShippingAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var appBarBinding: CustomAppbarProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var ordersAdapter: OrdersAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentUser: User? = null
    private var shippingAddress: ShippingAddress? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appBarBinding = CustomAppbarProfileBinding.bind(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupAppBar()
        setupClickListeners()
        setupOrdersRecyclerView()
        loadUserData()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun setupAppBar() {
        appBarBinding.toolbarTitle.text = "Профиль"
        appBarBinding.backButton.setOnClickListener {
            finish()
        }
        // Убираем иконку профиля из AppBar
        appBarBinding.profileIcon.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnManageAddresses.setOnClickListener {
            showAddressManagementDialog()
        }

        binding.btnExpandOrders.setOnClickListener {
            expandOrdersList()
        }
    }

    private fun setupOrdersRecyclerView() {
        ordersAdapter = OrdersAdapter(
            onOrderClick = { order ->
                openOrderDetails(order)
            }
        )

        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = ordersAdapter
        }
    }

    private fun loadUserData() {
        val firebaseUser = auth.currentUser
        firebaseUser?.let { user ->
            scope.launch {
                try {
                    binding.progressBar.visibility = View.VISIBLE

                    val userDoc = withContext(Dispatchers.IO) {
                        db.collection("users").document(user.uid).get().await()
                    }

                    if (userDoc.exists()) {
                        currentUser = userDoc.toObject(User::class.java)
                        withContext(Dispatchers.Main) {
                            currentUser?.let { displayUserData(it) }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            displayBasicUserData(user)
                        }
                    }

                    loadShippingAddress()
                    loadOrders()

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                        displayBasicUserData(user)
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun loadShippingAddress() {
        val userId = auth.currentUser?.uid ?: return

        scope.launch {
            try {
                // Пытаемся загрузить из подколлекции addresses
                val addressDoc = withContext(Dispatchers.IO) {
                    db.collection("users")
                        .document(userId)
                        .collection("addresses")
                        .document("default")
                        .get()
                        .await()
                }

                if (addressDoc.exists()) {
                    shippingAddress = addressDoc.toObject(ShippingAddress::class.java)
                    withContext(Dispatchers.Main) {
                        displayShippingAddress()
                    }
                } else {
                    // Если нет в подколлекции, пробуем загрузить из документа пользователя
                    val userDoc = withContext(Dispatchers.IO) {
                        db.collection("users").document(userId).get().await()
                    }

                    val addressMap = userDoc.get("shippingAddress") as? Map<String, Any>
                    if (addressMap != null) {
                        shippingAddress = ShippingAddress(
                            fullName = addressMap["fullName"] as? String ?: "",
                            addressLine1 = addressMap["addressLine1"] as? String ?: "",
                            addressLine2 = addressMap["addressLine2"] as? String ?: "",
                            city = addressMap["city"] as? String ?: "",
                            state = addressMap["state"] as? String ?: "",
                            postalCode = addressMap["postalCode"] as? String ?: "",
                            country = addressMap["country"] as? String ?: "",
                            phoneNumber = addressMap["phoneNumber"] as? String ?: ""
                        )
                        withContext(Dispatchers.Main) {
                            displayShippingAddress()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            binding.tvShippingAddress.text = getString(R.string.address_not_set)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvShippingAddress.text = getString(R.string.address_not_set)
                }
            }
        }
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.uid ?: return

        scope.launch {
            try {
                val ordersQuery = withContext(Dispatchers.IO) {
                    db.collection("users").document(userId).collection("orders")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(5)
                        .get()
                        .await()
                }

                val orders = ordersQuery.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }

                withContext(Dispatchers.Main) {
                    if (orders.isNotEmpty()) {
                        ordersAdapter.updateOrders(orders)
                        binding.tvOrdersCount.text = getString(R.string.total_orders, orders.size)
                        binding.tvRecentOrders.visibility = View.VISIBLE
                        binding.tvRecentOrders.text = getString(R.string.recent_orders)
                    } else {
                        binding.tvRecentOrders.text = getString(R.string.no_orders)
                        binding.recyclerViewOrders.visibility = View.GONE
                        binding.btnExpandOrders.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvRecentOrders.text = getString(R.string.error_loading_orders)
                }
            }
        }
    }

    private fun expandOrdersList() {
        if (binding.recyclerViewOrders.visibility == View.GONE) {
            loadAllOrders()
            binding.recyclerViewOrders.visibility = View.VISIBLE
            binding.btnExpandOrders.setImageResource(R.drawable.ic_collapse)
        } else {
            binding.recyclerViewOrders.visibility = View.GONE
            binding.btnExpandOrders.setImageResource(R.drawable.ic_expand)
        }
    }

    private fun loadAllOrders() {
        val userId = auth.currentUser?.uid ?: return

        scope.launch {
            try {
                val ordersQuery = withContext(Dispatchers.IO) {
                    db.collection("users").document(userId).collection("orders")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .await()
                }

                val orders = ordersQuery.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }

                withContext(Dispatchers.Main) {
                    ordersAdapter.updateOrders(orders)
                    binding.tvOrdersCount.text = getString(R.string.total_orders, orders.size)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Ошибка загрузки заказов", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayUserData(user: User) {
        binding.tvUserName.text = user.fullName
        binding.tvUserEmail.text = user.email
        binding.tvProvider.text = getString(R.string.login_method, user.provider)
        binding.tvEmailVerified.text = getString(R.string.email_verified,
            if (user.emailVerified) getString(R.string.yes) else getString(R.string.no))

        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        binding.tvCreatedAt.text = getString(R.string.account_created, dateFormat.format(user.createdAt))
        binding.tvLastLogin.text = getString(R.string.last_login, dateFormat.format(user.lastLogin))

        binding.tvUserRole.text = when (user.role) {
            "admin" -> getString(R.string.admin)
            else -> getString(R.string.customer)
        }

        // Загружаем аватар на странице
        if (user.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.profileAvatar)
        }
    }

    private fun displayShippingAddress() {
        shippingAddress?.let { address ->
            val addressText = buildString {
                append(address.fullName)
                append("\n${address.addressLine1}")
                if (address.addressLine2.isNotEmpty()) {
                    append("\n${address.addressLine2}")
                }
                append("\n${address.city}, ${address.state} ${address.postalCode}")
                append("\n${address.country}")
                append("\n${getString(R.string.phone)}: ${address.phoneNumber}")
            }
            binding.tvShippingAddress.text = addressText
        } ?: run {
            binding.tvShippingAddress.text = getString(R.string.address_not_set)
        }
    }

    private fun displayBasicUserData(user: com.google.firebase.auth.FirebaseUser) {
        binding.tvUserName.text = user.displayName ?: getString(R.string.user)
        binding.tvUserEmail.text = user.email ?: getString(R.string.not_specified)
        binding.tvProvider.text = getString(R.string.login_method, getString(R.string.unknown))
        binding.tvEmailVerified.text = getString(R.string.email_verified,
            if (user.isEmailVerified) getString(R.string.yes) else getString(R.string.no))
        binding.tvCreatedAt.text = getString(R.string.account_created, getString(R.string.unknown))
        binding.tvLastLogin.text = getString(R.string.last_login, getString(R.string.unknown))
        binding.tvUserRole.text = getString(R.string.customer)
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val editName: EditText = dialogView.findViewById(R.id.editName)

        editName.setText(binding.tvUserName.text.toString())

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_profile)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = editName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUserName(newName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateUserName(newName: String) {
        val userId = auth.currentUser?.uid ?: return

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.collection("users").document(userId)
                        .update("fullName", newName)
                        .await()
                }

                withContext(Dispatchers.Main) {
                    binding.tvUserName.text = newName
                    Toast.makeText(this@ProfileActivity, R.string.name_updated, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, R.string.error_update, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddressManagementDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_shipping_address, null)

        val editFullName: EditText = dialogView.findViewById(R.id.editFullName)
        val editAddressLine1: EditText = dialogView.findViewById(R.id.editAddressLine1)
        val editAddressLine2: EditText = dialogView.findViewById(R.id.editAddressLine2)
        val editCity: EditText = dialogView.findViewById(R.id.editCity)
        val editState: EditText = dialogView.findViewById(R.id.editState)
        val editPostalCode: EditText = dialogView.findViewById(R.id.editPostalCode)
        val editCountry: EditText = dialogView.findViewById(R.id.editCountry)
        val editPhone: EditText = dialogView.findViewById(R.id.editPhone)

        // Заполняем текущими данными
        shippingAddress?.let { address ->
            editFullName.setText(address.fullName)
            editAddressLine1.setText(address.addressLine1)
            editAddressLine2.setText(address.addressLine2)
            editCity.setText(address.city)
            editState.setText(address.state)
            editPostalCode.setText(address.postalCode)
            editCountry.setText(address.country)
            editPhone.setText(address.phoneNumber)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.shipping_address)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val address = ShippingAddress(
                    fullName = editFullName.text.toString().trim(),
                    addressLine1 = editAddressLine1.text.toString().trim(),
                    addressLine2 = editAddressLine2.text.toString().trim(),
                    city = editCity.text.toString().trim(),
                    state = editState.text.toString().trim(),
                    postalCode = editPostalCode.text.toString().trim(),
                    country = editCountry.text.toString().trim(),
                    phoneNumber = editPhone.text.toString().trim()
                )
                saveShippingAddress(address)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveShippingAddress(address: ShippingAddress) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("ProfileActivity", "Saving address for user: $userId")
        Log.d("ProfileActivity", "Address: $address")

        // Проверка на пустые поля
        if (address.fullName.isEmpty()) {
            Toast.makeText(this, "Введите полное имя", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.addressLine1.isEmpty()) {
            Toast.makeText(this, "Введите адрес", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.city.isEmpty()) {
            Toast.makeText(this, "Введите город", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.phoneNumber.isEmpty()) {
            Toast.makeText(this, "Введите телефон", Toast.LENGTH_SHORT).show()
            return
        }

        // Показываем индикатор загрузки
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Сохранение")
            .setMessage("Сохраняем адрес доставки...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val addressMap = hashMapOf(
                            "fullName" to address.fullName,
                            "addressLine1" to address.addressLine1,
                            "addressLine2" to address.addressLine2,
                            "city" to address.city,
                            "state" to address.state,
                            "postalCode" to address.postalCode,
                            "country" to address.country,
                            "phoneNumber" to address.phoneNumber
                        )

                        // Сохраняем в коллекцию addresses
                        db.collection("users")
                            .document(userId)
                            .collection("addresses")
                            .document("default")
                            .set(addressMap)
                            .await()

                        // Также сохраняем в самом документе пользователя для быстрого доступа
                        db.collection("users")
                            .document(userId)
                            .update("shippingAddress", addressMap)
                            .await()

                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (result) {
                        shippingAddress = address
                        displayShippingAddress()
                        Toast.makeText(this@ProfileActivity, "Адрес сохранен", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Ошибка сохранения адреса", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@ProfileActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // В ProfileActivity.kt, метод openOrderDetails
    private fun openOrderDetails(order: Order) {
        try {
            println("DEBUG: Opening order details for order: ${order.id}")
            println("DEBUG: Order total: ${order.totalAmount}")
            println("DEBUG: Order status: ${order.status}")
            println("DEBUG: Order items count: ${order.items.size}")

            val intent = Intent(this, OrderDetailActivity::class.java).apply {
                putExtra("ORDER_ID", order.id)
                putExtra("ORDER_TOTAL", order.totalAmount)
                putExtra("ORDER_STATUS", order.status)
                putExtra("ORDER_DATE", order.createdAt.time)
            }

            // Добавляем флаги для отладки
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        } catch (e: Exception) {
            println("DEBUG: Error opening order details: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Ошибка открытия заказа: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}