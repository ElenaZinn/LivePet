package com.example.aardemo.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.example.aardemo.R
import android.graphics.drawable.Drawable
import android.widget.ImageView


class UploadAreaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var isDragActive = false
    private var isImageSelected = false
    private val rect = RectF()
    private val path = Path()

    // Border paint
    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.upload_area_stroke_width) // 4dp
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
        color = ContextCompat.getColor(context, R.color.gray_dotted)
    }

    // Plus icon paint
    private val plusPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.upload_area_stroke_width) // 4dp
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        color = ContextCompat.getColor(context, R.color.gray_dotted)
        strokeCap = Paint.Cap.ROUND
    }

    init {
        setWillNotDraw(false)
        setupDragListener()
        setupClickListener()
    }

    private fun setupDragListener() {
        setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    isDragActive = true
                    invalidate()
                    true
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    animateHighlight(true)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    animateHighlight(false)
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    isDragActive = false
                    invalidate()
                    true
                }
                DragEvent.ACTION_DROP -> {
                    handleDrop(event)
                    true
                }
                else -> true
            }
        }
    }

    private fun setupClickListener() {
        setOnClickListener {
            if (!isImageSelected) {
                performClick()
            }
        }
    }

    private fun handleDrop(event: DragEvent) {
        isDragActive = false
        isImageSelected = true
        invalidate()
        (event.localState as? Uri)?.let { uri ->
            onImageDropped?.invoke(uri)
        }
    }

    private fun animateHighlight(highlight: Boolean) {
        val scale = if (highlight) 1.02f else 1f
        animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(150)
            .start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateRect()
    }

    private fun updateRect() {
        val strokeHalf = borderPaint.strokeWidth / 2
        rect.set(
            strokeHalf,
            strokeHalf,
            width.toFloat() - strokeHalf,
            height.toFloat() - strokeHalf
        )

        path.reset()
        val cornerRadius = resources.getDimension(R.dimen.upload_area_corner_radius)
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw dotted border
        borderPaint.color = when {
            isDragActive -> ContextCompat.getColor(context, R.color.light_close_background)
            isImageSelected -> ContextCompat.getColor(context, R.color.theme_blue)
            else -> ContextCompat.getColor(context, R.color.gray_dotted)
        }
        canvas.drawPath(path, borderPaint)

        // Draw plus icon if no image is selected
        if (!isImageSelected && findViewById<ImageView>(R.id.mainPreviewImage).visibility != View.VISIBLE) {
            drawPlusIcon(canvas)
        }
    }

    private fun drawPlusIcon(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val iconSize = 60f  // Updated to 60dp
        val halfIconSize = iconSize / 2

        // Draw horizontal dotted line
        canvas.drawLine(
            centerX - halfIconSize,
            centerY,
            centerX + halfIconSize,
            centerY,
            plusPaint
        )

        // Draw vertical dotted line
        canvas.drawLine(
            centerX,
            centerY - halfIconSize,
            centerX,
            centerY + halfIconSize,
            plusPaint
        )
    }

    fun setImageSelected(selected: Boolean) {
        if (isImageSelected != selected) {
            isImageSelected = selected
            invalidate()
        }
    }

    var onImageDropped: ((Uri) -> Unit)? = null

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }


    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.7f
        invalidate()
    }
}
