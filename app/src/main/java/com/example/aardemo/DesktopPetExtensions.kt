package com.example.aardemo

import android.animation.ObjectAnimator
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.CycleInterpolator
import android.widget.ImageView
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


fun ImageView.loadImage(uri: Uri?) {
    uri ?: return
    Glide.with(context)
        .load(uri)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                Log.e("ImageLoading", "Error loading image: ${e?.message}")
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                Log.d("ImageLoading", "Image loaded successfully")
                return false
            }
        })
        .into(this)
}


// ViewExtensions.kt
fun View.shake() {
    val animator = ObjectAnimator.ofFloat(this, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
    animator.duration = 500
    animator.interpolator = CycleInterpolator(5f)
    animator.start()
}

fun View.bounce() {
    val animator = ObjectAnimator.ofFloat(this, "translationY", 0f, -30f, 0f)
    animator.duration = 500
    animator.interpolator = BounceInterpolator()
    animator.start()
}