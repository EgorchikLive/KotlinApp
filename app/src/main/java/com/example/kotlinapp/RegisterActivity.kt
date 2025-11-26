package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kotlinapp.databinding.ActivityRegisterBinding
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val fullName = binding.etFullName.text.toString().trim()

        if (validateInputs(email, password, confirmPassword, fullName)) {
            binding.progressBar.visibility = android.view.View.VISIBLE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser

                        // Обновляем display name в Firebase Auth
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // Сохраняем пользователя в Firestore
                                saveUserToFirestore(user!!.uid, email, fullName)
                            } else {
                                binding.progressBar.visibility = android.view.View.GONE
                                Toast.makeText(
                                    this,
                                    "Ошибка обновления профиля: ${profileTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(
                            this,
                            "Ошибка регистрации: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun saveUserToFirestore(uid: String, email: String, fullName: String) {
        val user = User(
            uid = uid,
            email = email,
            fullName = fullName,
            createdAt = Date(),
            lastLogin = Date()
        )

        db.collection("users")
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(
                    this,
                    "Ошибка сохранения данных: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String
    ): Boolean {
        return when {
            fullName.isEmpty() -> {
                binding.etFullName.error = "Введите ФИО"
                false
            }
            email.isEmpty() -> {
                binding.etEmail.error = "Введите email"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Введите корректный email"
                false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Введите пароль"
                false
            }
            password.length < 6 -> {
                binding.etPassword.error = "Пароль должен содержать минимум 6 символов"
                false
            }
            confirmPassword.isEmpty() -> {
                binding.etConfirmPassword.error = "Подтвердите пароль"
                false
            }
            password != confirmPassword -> {
                binding.etConfirmPassword.error = "Пароли не совпадают"
                false
            }
            else -> true
        }
    }
}