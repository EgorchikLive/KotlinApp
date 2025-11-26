package com.example.kotlinapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")

    // Получить все товары
    suspend fun getAllProducts(): List<Product> {
        return try {
            productsCollection
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Product::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Добавить товар
    suspend fun addProduct(product: Product): Boolean {
        return try {
            if (product.id.isEmpty()) {
                // Новый товар - генерируем ID
                productsCollection.add(product).await()
            } else {
                // Обновление существующего товара
                productsCollection.document(product.id).set(product).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Обновить товар
    suspend fun updateProduct(product: Product): Boolean {
        return try {
            productsCollection.document(product.id).set(product).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Удалить товар
    suspend fun deleteProduct(productId: String): Boolean {
        return try {
            productsCollection.document(productId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Массовая загрузка товаров
    suspend fun bulkAddProducts(products: List<Product>): Boolean {
        return try {
            val batch = db.batch()

            products.forEach { product ->
                val docRef = if (product.id.isEmpty()) {
                    productsCollection.document()
                } else {
                    productsCollection.document(product.id)
                }
                batch.set(docRef, product.copy(id = docRef.id))
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}