package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.example.kotlinapp.databinding.ActivityMainBinding
import com.example.kotlinapp.databinding.CustomAppbarBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarBinding: CustomAppbarBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем binding для кастомного AppBar
        appBarBinding = CustomAppbarBinding.bind(binding.root)

        auth = FirebaseAuth.getInstance()

        // Проверяем авторизацию
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupAppBar()
        setupBottomNavigation()
    }

    private fun setupAppBar() {
        // Устанавливаем начальный заголовок
        appBarBinding.toolbarTitle.text = "Главная"

        // Настраиваем клик на иконку профиля
        appBarBinding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Принудительно выравниваем элементы по вертикали
        appBarBinding.root.post {
            // Убеждаемся, что текст и иконка на одном уровне
            val textView = appBarBinding.toolbarTitle
            val imageButton = appBarBinding.profileButton

            // Выравниваем по центру вертикали родительского контейнера
            textView.gravity = android.view.Gravity.CENTER_VERTICAL
        }
    }

    private fun setupBottomNavigation() {
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    appBarBinding.toolbarTitle.text = "Главная"
                    true
                }
                R.id.navigation_cart -> {
                    replaceFragment(CartFragment())
                    appBarBinding.toolbarTitle.text = "Корзина"
                    true
                }
                R.id.navigation_favorites -> {
                    replaceFragment(FavoritesFragment())
                    appBarBinding.toolbarTitle.text = "Избранное"
                    true
                }
                else -> false
            }
        }

        // Устанавливаем начальный фрагмент
        replaceFragment(HomeFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()
    }
}