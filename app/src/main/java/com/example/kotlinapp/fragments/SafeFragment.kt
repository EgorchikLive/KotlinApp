package com.example.kotlinapp

import androidx.fragment.app.Fragment
import kotlinx.coroutines.*

open class SafeFragment : Fragment() {
    private val job = SupervisorJob()
    protected val uiScope = CoroutineScope(Dispatchers.Main + job)
    protected val ioScope = CoroutineScope(Dispatchers.IO + job)

    override fun onDestroyView() {
        super.onDestroyView()
        job.cancel() // Отменяем все корутины при уничтожении view
    }

    // Безопасный запуск корутины с обработкой отмены
    protected fun safeLaunch(block: suspend CoroutineScope.() -> Unit): Job {
        return uiScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                // Игнорируем отмену - это нормально при переключении фрагментов
                throw e
            } catch (e: Exception) {
                // Логируем другие ошибки, но не крашим приложение
                e.printStackTrace()
            }
        }
    }
}