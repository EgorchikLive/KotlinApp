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

    suspend fun addProduct(product: Product): Product? {
        return try {
            // Создаем новый документ с автоматическим ID
            val newDocRef = productsCollection.document()

            // Создаем продукт с новым ID
            val productWithId = product.copy(id = newDocRef.id)

            // Сохраняем в Firestore
            newDocRef.set(productWithId).await()

            // Возвращаем сохраненный продукт с ID
            productWithId
        } catch (e: Exception) {
            null
        }
    }

    // Альтернативный метод для обновления существующего продукта
    suspend fun updateProduct(product: Product): Boolean {
        return try {
            if (product.id.isEmpty()) {
                // Если ID пустой, создаем новый продукт
                addProduct(product) != null
            } else {
                // Обновляем существующий
                productsCollection.document(product.id).set(product).await()
                true
            }
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
                val docRef = productsCollection.document()
                val productWithId = product.copy(id = docRef.id)
                batch.set(docRef, productWithId)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return try {
            productsCollection.document(productId).get().await().toObject(Product::class.java)
        } catch (e: Exception) {
            null
        }
    }
}