package com.example.aardemo.operation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.aardemo.Constants
import com.example.aardemo.R
import com.example.aardemo.databinding.ActivityOperationBinding
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File
import java.io.FileOutputStream
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.aardemo.Constants.DEFAULT_SIZE
import com.example.aardemo.Constants.IMAGE_UPDATE_INTERVAL
import com.example.aardemo.Constants.MAX_SIZE
import com.example.aardemo.Constants.MIN_SIZE
import com.example.aardemo.utils.MovementController
import com.example.aardemo.utils.RandomOrderController
import java.util.Random
import com.example.aardemo.MainActivity
import com.example.aardemo.PetService
import com.example.aardemo.imagepicker.ImageItem
import com.example.aardemo.imagepicker.ImageSelectionActivity
import com.example.aardemo.imagepicker.ImageSelectionActivity.Companion.REMAINING_SLOTS
import com.example.aardemo.utils.PermissionNewUtils


class OperationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOperationBinding
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var previewAdapter: ImagePreviewAdapter
    private var currentMainImageUri: Uri? = null
    private var initialRandomPosition = false
    private var initialRandomOrder = false
    private var currentSize = DEFAULT_SIZE

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private val imageUpdateRunnable = object : Runnable {
        override fun run() {
            if (RandomOrderController.isRandomOrderEnabled()) {
                updateMainPreviewRandomly()
            }
            handler.postDelayed(this, IMAGE_UPDATE_INTERVAL)
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val MAX_IMAGES = 9
        const val PERMISSION_REQUEST_CODE = 100
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Stop existing service first if it's running
            stopService(Intent(this, PetService::class.java))
        } catch (e: Exception) {
            Log.e("OperationActivity", "Error handling image: ${e.message}")
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_operation)
        PermissionNewUtils.requestPermission(this)
        setupViews()
        setupRecyclerView()
        loadSavedSettings()
    }

    private fun setupViews() {
        binding.apply {
            // Title bar setup

            finishButton.setOnClickListener { saveAndFinish() }

            // Upload area setup
            uploadArea.setOnClickListener {
                openImagePicker()
            }

            // Settings icons setup - default disabled state
            updateIconBackground(randomPositionIcon, false)
            updateIconBackground(randomOrderIcon, false)

            randomPositionIcon.setOnClickListener {
                val isEnabled = MovementController.toggleMovement()
                updateIconBackground(randomPositionIcon, isEnabled)
            }

            randomOrderIcon.setOnClickListener {
                val isEnabled = RandomOrderController.toggleRandomOrder()
                updateIconBackground(randomOrderIcon, isEnabled)
                if (isEnabled) {
                    startImageUpdateTimer()
                } else {
                    stopImageUpdateTimer()
                    // Reset to first image when disabled
                    updateMainPreview()
                }
            }

            sizeIcon.setOnClickListener {
                showSizeDialog()
            }

            // 扩大选取
            imageCountText.setOnClickListener{
                showEditOrderTipDialog()
            }

            imageCountTip.setOnClickListener{
                    showEditOrderTipDialog()
            }
        }
    }

    private fun setupRecyclerView() {
        previewAdapter = ImagePreviewAdapter(
            onImageRemoved = { position ->
                if (position < selectedImages.size) {
                    selectedImages.removeAt(position)
                    updateUploadAreaState()
                    if (selectedImages.isEmpty() || position == 0) {
                        updateMainPreview()
                    }
                }
            },
            onImageClick = { position ->
//                showImagePreviewDialog(position)
            },
            onImageReordered = { fromPosition, toPosition ->
                if (fromPosition < selectedImages.size && toPosition < selectedImages.size) {
                    val image = selectedImages.removeAt(fromPosition)
                    selectedImages.add(toPosition, image)
                    if (fromPosition == 0 || toPosition == 0) {
                        updateMainPreview()
                    }
                }
            }
        )

        binding.imagePreviewRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@OperationActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = previewAdapter
            ItemTouchHelper(ImageTouchCallback(previewAdapter)).attachToRecyclerView(this)
            // Add spacing between items
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    outRect.left = if (position == 0) 8 else 4
                    outRect.right = if (position == state.itemCount - 1) 8 else 4
                }
            })
        }
    }

    private fun addItemDecoration(any: Any) {

    }

    private fun handleNewImage(uri: Uri) {
        if (selectedImages.size >= MAX_IMAGES) {
            return
        }

        try {
            val copiedUri = copyImageToInternalStorage(uri)
            selectedImages.add(copiedUri)
            Log.d("OperationActivity", "Image added. Total: ${selectedImages.size}")
        } catch (e: Exception) {
            Log.e("OperationActivity", "Error handling image: ${e.message}")
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): Uri {
        try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

            val fileName = "pet_image_${System.currentTimeMillis()}.jpg"
            val outputFile = File(filesDir, fileName)

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Use FileProvider to create content URI
            return FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            Log.e("OperationActivity", "Error copying image: ${e.message}")
            throw e
        }
    }


    private fun getFileExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return when {
            mimeType?.contains("gif") == true -> "gif"
            else -> "jpg"
        }
    }

    private fun updateMainPreview() {
        binding.apply {
            if (selectedImages.isNotEmpty()) {
                // If random order is enabled, pick a random image
                currentMainImageUri = if (RandomOrderController.isRandomOrderEnabled()) {
                    selectedImages[random.nextInt(selectedImages.size)]
                } else {
                    selectedImages[0]
                }
                mainPreviewImage.visibility = View.VISIBLE

                try {
                    // Clear existing image first
                    mainPreviewImage.setImageDrawable(null)

                    // Load new image
                    Glide.with(this@OperationActivity)
                        .load(currentMainImageUri)
                        .centerCrop()
                        .override(1024, 1024)  // Higher resolution
                        .fitCenter()           // Better scaling
                        .diskCacheStrategy(DiskCacheStrategy.ALL)  // Better caching
                        .error(R.drawable.ic_error_image)
                        .into(mainPreviewImage)

                    uploadArea.setImageSelected(true)
                } catch (e: Exception) {
                    Log.e("OperationActivity", "Error in updateMainPreview: ${e.message}")
                    mainPreviewImage.visibility = View.GONE
                    uploadArea.setImageSelected(false)
                }
            } else {
                mainPreviewImage.visibility = View.GONE
                uploadArea.setImageSelected(false)
                stopImageUpdateTimer()
            }
        }
    }


    private fun updateUploadAreaState() {
        binding.apply {
            uploadArea.setImageSelected(selectedImages.isNotEmpty())

            // Disable upload area if max images reached
            uploadArea.isEnabled = selectedImages.size < MAX_IMAGES

            // Update image counter
            imageCountText.text = "${selectedImages.size}/$MAX_IMAGES ${getString(R.string.pet_selected_image_number)}"

            // Update upload prompt if needed
            if (selectedImages.size > MAX_IMAGES) {
                showToast("Maximum number of images reached")
            }
        }
    }

    private fun updateIconBackground(icon: FrameLayout, isEnabled: Boolean) {
        icon.background = ContextCompat.getDrawable(
            this,
            if (isEnabled) R.drawable.circle_background_blue
            else R.drawable.circle_background_gray
        )
    }

    private fun saveAndFinish() {
        if (selectedImages.isEmpty()) {
            showToast("Please select at least one image")
            return
        }

        try {
            // Stop existing service first if it's running
            stopService(Intent(this, PetService::class.java))

            // Save images
            getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).edit().apply {
                putStringSet(Constants.KEY_IMAGE_URIS,
                    selectedImages.map { it.toString() }.toSet())
                putBoolean(Constants.KEY_RANDOM_POSITION, MovementController.isMovementEnabled())
                putBoolean(Constants.KEY_RANDOM_ORDER, RandomOrderController.isRandomOrderEnabled())
                putInt(Constants.KEY_PET_SIZE, currentSize)  // Save size
                apply()
            }

            setResult(Activity.RESULT_OK)

            // Start MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)

            // Optional: add transition animation
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

            // Close OperationActivity
            finish()
        } catch (e: Exception) {
            Log.e("OperationActivity", "Error saving settings: ${e.message}")
            showToast("Error saving settings")
        }
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        // Load saved images
        prefs.getStringSet(Constants.KEY_IMAGE_URIS, setOf())?.forEach { uriString ->
            try {
                selectedImages.add(Uri.parse(uriString))
            } catch (e: Exception) {
                Log.e("OperationActivity", "Error parsing URI: $uriString")
            }
        }

        previewAdapter.submitList(selectedImages.toList())
        // Load settings
        binding.apply {
            val isRandomOrder = prefs.getBoolean(Constants.KEY_RANDOM_ORDER, false)
            RandomOrderController.setEnabled(isRandomOrder)
            updateIconBackground(randomOrderIcon, isRandomOrder)

            if (isRandomOrder && selectedImages.isNotEmpty()) {
                startImageUpdateTimer()
            } else {
                updateMainPreview()
            }
        }

        // Load size with new bounds
        currentSize = prefs.getInt(Constants.KEY_PET_SIZE, Constants.DEFAULT_SIZE)
            .coerceIn(Constants.MIN_SIZE, Constants.MAX_SIZE)
        updateMainPreviewSize(currentSize)

        updateUploadAreaState()

        // Load and store initial settings state
        initialRandomPosition = prefs.getBoolean(Constants.KEY_RANDOM_POSITION, false)
        initialRandomOrder = prefs.getBoolean(Constants.KEY_RANDOM_ORDER, false)
        // Load size
        currentSize = prefs.getInt(Constants.KEY_PET_SIZE, DEFAULT_SIZE)

        // Apply settings
        MovementController.setEnabled(initialRandomPosition)
        RandomOrderController.setEnabled(initialRandomOrder)

        // Update UI
        updateIconBackground(binding.randomPositionIcon, initialRandomPosition)
        updateIconBackground(binding.randomOrderIcon, initialRandomOrder)
        updateMainPreviewSize(currentSize)
    }

    private fun showImagePreviewDialog(position: Int) {
        ImagePreviewDialog(
            this,
            selectedImages[position],
            position,
            selectedImages.size
        ).show()
    }

    private fun openImagePicker() {
        val remainingSlots = MAX_IMAGES - selectedImages.size
        if (remainingSlots <= 0) {
            showToast("Maximum ${MAX_IMAGES} images allowed")
            return
        }
        try {
            // Start ImageSelectionActivity
            val intent = Intent(this, ImageSelectionActivity::class.java).apply {
                putExtra(REMAINING_SLOTS, remainingSlots)
                // Optional: pass currently selected images if you want to show them as selected
                putParcelableArrayListExtra("current_selections",
                    ArrayList(selectedImages.map { uri ->
                        ImageItem(uri.hashCode().toLong(), uri)
                    })
                )
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        } catch (e: Exception) {
            Log.e("OperationActivity", "Error opening image picker: ${e.message}")
            showToast("Error opening image picker")
        }
    }

    private fun showUnsavedChangesDialog(onConfirm: () -> Unit) {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to discard them?")
                .setPositiveButton("Discard") { _, _ -> onConfirm() }
                .setNegativeButton("Keep Editing", null)
                .show()
        } else {
            onConfirm()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val savedImages = prefs.getStringSet(Constants.KEY_IMAGE_URIS, setOf()) ?: setOf()
        val currentImages = selectedImages.map { it.toString() }.toSet()

        return savedImages != currentImages ||
                MovementController.isMovementEnabled() != initialRandomPosition ||
                RandomOrderController.isRandomOrderEnabled() != initialRandomOrder
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.let { intent ->
                // Get selected images from ImageSelectionActivity
                val selectedItems = intent.getParcelableArrayListExtra<ImageItem>(
                    ImageSelectionActivity.EXTRA_SELECTED_IMAGES
                )

                selectedItems?.forEach { imageItem ->
                    handleNewImage(imageItem.uri)
                }

                // Update UI
                previewAdapter.submitList(selectedImages.toList())
                updateUploadAreaState()
                if (selectedImages.size > 0) {
                    updateMainPreview()
                }
            }
        }
    }


    override fun onBackPressed() {
        showUnsavedChangesDialog { super.onBackPressed() }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temporary files if needed
        selectedImages.clear()
        stopImageUpdateTimer()
    }

    /**
     * support randomly
     */

    private fun startImageUpdateTimer() {
        handler.removeCallbacks(imageUpdateRunnable) // Remove any existing callbacks
        handler.post(imageUpdateRunnable)
    }

    private fun stopImageUpdateTimer() {
        handler.removeCallbacks(imageUpdateRunnable)
    }

    private fun updateMainPreviewRandomly() {
        if (selectedImages.isNotEmpty()) {
            val randomIndex = random.nextInt(selectedImages.size)
            currentMainImageUri = selectedImages[randomIndex]
            binding.apply {
                mainPreviewImage.apply {
                    visibility = View.VISIBLE
                    Glide.with(this@OperationActivity)
                        .load(currentMainImageUri)
                        .into(this)
                }
            }
        }
    }

    // Override onResume and onPause to handle the timer
    override fun onResume() {
        super.onResume()
        if (RandomOrderController.isRandomOrderEnabled()) {
            startImageUpdateTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        stopImageUpdateTimer()
    }

    private fun showSizeDialog() {
        SizeSettingDialog(
            ContextThemeWrapper(this, R.style.RoundedDialog),  // Apply theme her,
            currentSize
        ) { newSize ->
            currentSize = newSize.coerceIn(
                MIN_SIZE,
                MAX_SIZE
            )
            updateMainPreviewSize(currentSize)
        }.show()
    }


    private fun updateMainPreviewSize(size: Int) {

        val params = binding.mainPreviewImage.layoutParams as ConstraintLayout.LayoutParams
        val newSize = dpToPx(size.coerceIn(MIN_SIZE, MAX_SIZE))
        params.width = newSize
        params.height = newSize
        binding.mainPreviewImage.layoutParams = params

        // Force layout update
        binding.mainPreviewImage.requestLayout()
        binding.uploadArea.invalidate()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }


    private fun showEditOrderTipDialog() {
        EditOrderTipDialog(
            ContextThemeWrapper(this, R.style.RoundedDialog),  // Apply theme her,
        ).show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Photo access permission is required. Please enable it in app settings.")
            .setPositiveButton("Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showToast("Permission is required to select images")
            }
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do nothing
                } else {
                    if (!shouldShowRequestPermissionRationale(permissions[0])) {
                        // Permission permanently denied
                        showPermissionSettingsDialog()
                    } else {
                        showToast("Permission denied. Cannot select images.")
                    }
                }
            }
        }
    }


}
