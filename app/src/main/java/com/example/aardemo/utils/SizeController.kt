package com.example.aardemo.utils

import com.example.aardemo.Constants

object SizeController {
    private var size: Int = Constants.DEFAULT_SIZE
    private var listeners = mutableListOf<(Int) -> Unit>()

    fun getSize(): Int = size

    fun setSize(newSize: Int) {
        size = newSize
        notifyListeners()
    }

    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Int) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it(size) }
    }
}
