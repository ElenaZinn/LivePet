package com.example.aardemo.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView

class UploadCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private var dashLength = 10f
    private var dashGap = 5f
    private var dashColor = Color.GRAY
    private val dashPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = dashColor
        pathEffect = DashPathEffect(floatArrayOf(dashLength, dashGap), 0f)
    }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = RectF(4f, 4f, width - 4f, height - 4f)
        canvas.drawRect(rect, dashPaint)
    }
}
