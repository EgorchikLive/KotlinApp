package com.example.kotlinapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.coroutineContext

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")

    suspend fun getAllProducts(): List<Product> {
        return try {
            if (!coroutineContext.isActive) return emptyList()
            productsCollection
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Product::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addProduct(product: Product): Boolean {
        return try {
            if (product.id.isEmpty()) {
                productsCollection.add(product).await()
            } else {
                productsCollection.document(product.id).set(product).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateProduct(product: Product): Boolean {
        return try {
            productsCollection.document(product.id).set(product).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteProduct(productId: String): Boolean {
        return try {
            productsCollection.document(productId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun bulkAddProducts(products: List<Product>): Boolean {
        return try {
            val batch = db.batch()

            products.forEach { product ->
                val docRef = if (product.id.isEmpty()) {
                    productsCollection.document()
                } else {
                    productsCollection.document(product.id)
                }
                // Убеждаемся, что сохраняем все поля продукта
                batch.set(docRef, product.copy(id = docRef.id))
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Добавляем метод для получения товара по ID
    suspend fun getProductById(productId: String): Product? {
        return try {
            productsCollection.document(productId).get().await().toObject(Product::class.java)
        } catch (e: Exception) {
            null
        }
    }
}