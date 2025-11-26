package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kotlinapp.databinding.ActivityLoginBinding
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Настройка Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Добавьте в strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Если пользователь уже авторизован, переходим на главный экран
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun signInWithGoogle() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Ошибка входа через Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // Сохраняем/обновляем пользователя в Firestore
                        saveOrUpdateGoogleUser(it)
                    }
                } else {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Ошибка аутентификации: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveOrUpdateGoogleUser(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            fullName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: "",
            provider = "google",
            createdAt = Date(), // Будет обновлено если пользователь уже существует
            lastLogin = Date(),
            emailVerified = firebaseUser.isEmailVerified
        )

        // Проверяем, существует ли пользователь
        db.collection("users")
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Обновляем только lastLogin для существующего пользователя
                    db.collection("users")
                        .document(firebaseUser.uid)
                        .update(
                            "lastLogin", Date(),
                            "emailVerified", firebaseUser.isEmailVerified,
                            "photoUrl", firebaseUser.photoUrl?.toString() ?: ""
                        )
                        .addOnSuccessListener {
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                } else {
                    // Создаем нового пользователя
                    db.collection("users")
                        .document(firebaseUser.uid)
                        .set(user)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Регистрация через Google успешна!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Ошибка сохранения данных: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Ошибка проверки пользователя: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (validateInputs(email, password)) {
            binding.progressBar.visibility = android.view.View.VISIBLE

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Обновляем время последнего входа в Firestore
                        updateLastLogin()
                    } else {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(
                            this,
                            "Ошибка авторизации: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun updateLastLogin() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .update(
                    "lastLogin", Date(),
                    "emailVerified", user.isEmailVerified
                )
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Авторизация успешна!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(
                        this,
                        "Ошибка обновления данных: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun resetPassword() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Введите email для восстановления"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Введите корректный email"
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = android.view.View.GONE

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Письмо для восстановления пароля отправлено на $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
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
            else -> true
        }
    }
}