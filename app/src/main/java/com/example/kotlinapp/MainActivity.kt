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

        appBarBinding = CustomAppbarBinding.bind(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupAppBar()
        setupBottomNavigation()
    }

    private fun setupAppBar() {
        appBarBinding.toolbarTitle.text = "Главная"
        appBarBinding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
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

        replaceFragment(HomeFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        // Используем commitNow вместо commit для немедленного выполнения
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }
}