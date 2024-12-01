package com.example.aardemo.utils

import java.util.Random

object RandomOrderController {
    private var isEnabled = false
    private var listeners = mutableListOf<(Boolean) -> Unit>()

    fun toggleRandomOrder(): Boolean {
        isEnabled = !isEnabled
        return isEnabled
    }

    fun isRandomOrderEnabled(): Boolean = isEnabled

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        notifyListeners()
    }


    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it(isEnabled) }
    }
}
