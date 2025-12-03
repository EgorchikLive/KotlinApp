package com.example.kotlinapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.coroutines.coroutineContext

class CartRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun getUserCartCollection() = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId).collection("cart")
    }

    suspend fun getCartItems(): List<CartItem> {
        return try {
            if (!coroutineContext.isActive) return emptyList()
            val cartCollection = getUserCartCollection()
            cartCollection?.get()?.await()
                ?.toObjects(CartItem::class.java)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToCart(product: Product): Boolean {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) return false

            val cartCollection = db.collection("users").document(userId).collection("cart")

            val existingItemQuery = cartCollection
                .whereEqualTo("productId", product.id)
                .get()
                .await()

            return if (existingItemQuery.documents.isEmpty()) {
                // Товара еще нет в корзине - добавляем новый
                val cartItem = CartItem(
                    id = cartCollection.document().id,
                    productId = product.id,
                    productName = product.name,
                    productPrice = product.price,
                    productImageUrl = product.imageUrl,
                    // Не добавляем description и category
                    quantity = 1,
                    addedAt = Date()
                )
                cartCollection.document(cartItem.id).set(cartItem).await()
                true
            } else {
                // Товар уже есть - увеличиваем количество
                val existingDoc = existingItemQuery.documents.first()
                val existingQuantity = existingDoc.getLong("quantity")?.toInt() ?: 0
                existingDoc.reference.update("quantity", existingQuantity + 1).await()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // Остальные методы остаются без изменений...
    suspend fun updateCartItemQuantity(cartItemId: String, quantity: Int): Boolean {
        return try {
            if (quantity <= 0) {
                return removeFromCart(cartItemId)
            }

            val cartCollection = getUserCartCollection()
            cartCollection?.document(cartItemId)?.update("quantity", quantity)?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromCart(cartItemId: String): Boolean {
        return try {
            val cartCollection = getUserCartCollection()
            cartCollection?.document(cartItemId)?.delete()?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearCart(): Boolean {
        return try {
            val cartItems = getCartItems()
            val cartCollection = getUserCartCollection()

            cartItems.forEach { item ->
                cartCollection?.document(item.id)?.delete()?.await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCartTotal(): Double {
        return try {
            val cartItems = getCartItems()
            cartItems.sumOf { it.productPrice * it.quantity }
        } catch (e: Exception) {
            0.0
        }
    }
}