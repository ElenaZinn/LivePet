package com.example.aardemo.imagepicker

import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.aardemo.databinding.ActivityImageSelectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageSelectionBinding
    private lateinit var adapter: ImageSelectionAdapter
    private val selectedImages = mutableSetOf<ImageItem>()
    private var maxSelection: Int ? = 9

    companion object {
        const val EXTRA_SELECTED_IMAGES = "selected_images"
        const val REMAINING_SLOTS = "remaining_slots"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageSelectionBinding.inflate(layoutInflater)
        maxSelection = intent.getIntExtra(REMAINING_SLOTS, 9)
        setContentView(binding.root)

        setupViews()
        loadImages()
    }

    private fun setupViews() {
        adapter = ImageSelectionAdapter(
            maxSelection = maxSelection?:9,
            onImageSelected = { image, isSelected ->
                handleImageSelection(image, isSelected)
            },
            getSelectedImages = { selectedImages }
        )

        binding.apply {
            // Setup Grid
            imageGrid.apply {
                layoutManager = GridLayoutManager(this@ImageSelectionActivity, 3)
                adapter = this@ImageSelectionActivity.adapter
                addItemDecoration(GridSpacingItemDecoration(3, dpToPx(2), true))
            }

            // Setup Buttons
            backButton.setOnClickListener { finish() }
            confirmButton.setOnClickListener { confirmSelection() }

            // Initial state
            updateButtonState()
        }
    }

    private fun handleImageSelection(image: ImageItem, isSelected: Boolean) {
        if (isSelected) {
            if (selectedImages.size < maxSelection?:9) {
                selectedImages.add(image)
            } else {
                // Show max selection message
                Toast.makeText(this, "Maximum $maxSelection images allowed",
                    Toast.LENGTH_SHORT).show()
                adapter.notifyDataSetChanged() // Reset checkbox
                return
            }
        } else {
            selectedImages.remove(image)
        }
        updateButtonState()
    }

    private fun updateButtonState() {
        binding.apply {
            selectedCountText.text = "(${selectedImages.size}/$maxSelection)"
            confirmButton.text = "Confirm (${selectedImages.size})"
            confirmButton.isEnabled = selectedImages.isNotEmpty()
        }
    }

    private fun confirmSelection() {
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra(EXTRA_SELECTED_IMAGES,
                ArrayList(selectedImages))
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun loadImages() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Load images from device
                val images = getDeviceImages()
                withContext(Dispatchers.Main) {
                    adapter.submitList(images)
                }
            }
        }
    }

    private fun getDeviceImages(): List<ImageItem> {
        val images = mutableListOf<ImageItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                images.add(ImageItem(id, contentUri))
            }
        }

        return images
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
