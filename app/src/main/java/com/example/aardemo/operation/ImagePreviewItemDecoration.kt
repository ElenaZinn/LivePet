package com.example.aardemo.operation

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView


class ImagePreviewItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)

        // Add spacing between items
        outRect.left = if (position == 0) spacing * 2 else spacing
        outRect.right = if (position == state.itemCount - 1) spacing * 2 else spacing
        outRect.top = spacing
        outRect.bottom = spacing
    }
}

