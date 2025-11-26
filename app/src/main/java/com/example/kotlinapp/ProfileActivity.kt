package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kotlinapp.databinding.ActivityProfileBinding
import com.example.kotlinapp.databinding.CustomAppbarProfileBinding
import java.text.DateFormat

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var appBarBinding: CustomAppbarProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем binding для кастомного AppBar
        appBarBinding = CustomAppbarProfileBinding.bind(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupAppBar()
        loadUserData()
        setupClickListeners()
    }

    private fun setupAppBar() {
        appBarBinding.toolbarTitle.text = "Профиль"

        // Настраиваем кнопку назад
        appBarBinding.backButton.setOnClickListener {
            onBackPressed()
        }
    }

    // остальные методы без изменений...
    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userData = document.toObject(User::class.java)
                        userData?.let {
                            displayUserData(it)
                        }
                    } else {
                        displayBasicUserData(user)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    displayBasicUserData(user)
                }
        }
    }

    private fun displayUserData(user: User) {
        binding.tvUserName.text = user.fullName
        binding.tvUserEmail.text = user.email
        binding.tvProvider.text = "Способ входа: ${user.provider}"
        binding.tvEmailVerified.text = "Email подтвержден: ${if (user.emailVerified) "Да" else "Нет"}"

        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        binding.tvCreatedAt.text = "Аккаунт создан: ${dateFormat.format(user.createdAt)}"
        binding.tvLastLogin.text = "Последний вход: ${dateFormat.format(user.lastLogin)}"
    }

    private fun displayBasicUserData(user: com.google.firebase.auth.FirebaseUser) {
        binding.tvUserName.text = user.displayName ?: "Пользователь"
        binding.tvUserEmail.text = user.email ?: "Не указан"
        binding.tvProvider.text = "Способ входа: Неизвестно"
        binding.tvEmailVerified.text = "Email подтвержден: ${if (user.isEmailVerified) "Да" else "Нет"}"
        binding.tvCreatedAt.text = "Аккаунт создан: Неизвестно"
        binding.tvLastLogin.text = "Последний вход: Неизвестно"
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
            Toast.makeText(this, "Редактирование профиля", Toast.LENGTH_SHORT).show()
        }
    }
}