package com.example.aardemo.utils

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent

// PetGestureDetector.kt
class PetGestureDetector(
    context: Context,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onLongPress: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector = GestureDetector(context, this)

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        onSingleTap()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        onDoubleTap()
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        onLongPress()
    }
}
