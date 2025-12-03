package com.example.kotlinapp

import com.example.kotlinapp.models.FavoriteItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.coroutines.coroutineContext

class FavoritesRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun getUserFavoritesCollection() = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId).collection("favorites")
    }

    suspend fun getFavorites(): List<FavoriteItem> {
        return try {
            if (!coroutineContext.isActive) return emptyList()
            val favoritesCollection = getUserFavoritesCollection()
            favoritesCollection?.get()?.await()
                ?.toObjects(FavoriteItem::class.java)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToFavorites(product: Product): Boolean {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) return false

            val favoritesCollection = db.collection("users").document(userId).collection("favorites")

            val existingItemQuery = favoritesCollection
                .whereEqualTo("productId", product.id)
                .get()
                .await()

            return if (existingItemQuery.documents.isEmpty()) {
                val favoriteItem = FavoriteItem(
                    id = favoritesCollection.document().id,
                    productId = product.id,
                    productName = product.name,
                    productPrice = product.price,
                    productImageUrl = product.imageUrl,
                    // Не добавляем description и category
                    addedAt = Date()
                )
                favoritesCollection.document(favoriteItem.id).set(favoriteItem).await()
                true
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromFavorites(productId: String): Boolean {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) return false

            val favoritesCollection = db.collection("users").document(userId).collection("favorites")

            val query = favoritesCollection
                .whereEqualTo("productId", productId)
                .get()
                .await()

            if (!query.isEmpty) {
                val doc = query.documents.first()
                doc.reference.delete().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isProductInFavorites(productId: String): Boolean {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) return false

            val favoritesCollection = db.collection("users").document(userId).collection("favorites")

            val query = favoritesCollection
                .whereEqualTo("productId", productId)
                .get()
                .await()

            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFavoriteProducts(): List<Product> {
        return try {
            val favorites = getFavorites()
            val productIds = favorites.map { it.productId }

            if (productIds.isEmpty()) return emptyList()

            val products = mutableListOf<Product>()
            val productRepo = ProductRepository()
            val allProducts = productRepo.getAllProducts()

            favorites.forEach { favorite ->
                allProducts.find { it.id == favorite.productId }?.let { product ->
                    products.add(product)
                }
            }

            products
        } catch (e: Exception) {
            emptyList()
        }
    }
}