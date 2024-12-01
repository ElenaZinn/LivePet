package com.example.aardemo.operation

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import com.example.aardemo.R
import com.example.aardemo.databinding.DialogImagePreviewBinding
import com.example.aardemo.loadImage
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


// ImagePreviewDialog.kt
class ImagePreviewDialog(
    context: Context,
    private val imageUri: Uri,
    private val position: Int,
    private val total: Int
) : Dialog(context, R.style.FullScreenDialog) {

    private lateinit var binding: DialogImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDialog()
        loadImage()
        setupActions()
    }

    private fun setupDialog() {
        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.black)
        }
    }

    private fun loadImage() {
        binding.apply {
            imageCounter.text = "${position + 1}/$total"

            // Load image using Glide
            Glide.with(context)
                .load(imageUri)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ImagePreviewDialog", "Error loading image: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("ImagePreviewDialog", "Image loaded successfully")
                        return false
                    }
                })
                .into(previewImage)
        }
    }

    private fun setupActions() {
        binding.apply {
            closeButton.setOnClickListener { dismiss() }
            root.setOnClickListener { dismiss() }
            previewImage.setOnClickListener { /* Prevent dialog dismiss */ }
        }
    }
}
