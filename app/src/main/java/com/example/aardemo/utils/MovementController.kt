package com.example.aardemo.utils

import java.util.Random

// MovementController.kt
object MovementController {
    private var isEnabled = false
    private val random = Random()

    fun toggleMovement(): Boolean {
        isEnabled = !isEnabled
        return isEnabled
    }

    fun isMovementEnabled(): Boolean = isEnabled

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun calculateNextPosition(
        screenWidth: Int,
        screenHeight: Int,
        viewWidth: Int,
        viewHeight: Int
    ): Pair<Int, Int> {
        if (!isEnabled) return Pair(-1, -1)

        val newX = random.nextInt(screenWidth - viewWidth)
        val newY = random.nextInt(screenHeight - viewHeight)
        return Pair(newX, newY)
    }

}
