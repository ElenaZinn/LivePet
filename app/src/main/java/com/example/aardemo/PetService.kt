package com.example.aardemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.aardemo.Constants.ANIMATION_INTERVAL
import com.example.aardemo.utils.MovementController
import com.example.aardemo.utils.RandomOrderController
import java.util.Random

class PetService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var petView: View
    private lateinit var petImageView: ImageView
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Add this property to track current image
    private var currentUri: Uri? = null

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private var isViewCreated = false  // Use this flag instead of isInitialized

    companion object {
        private const val CHANNEL_ID = "PetServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private fun updatePetImage() {
        try {
            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val imageUris = prefs.getStringSet(Constants.KEY_IMAGE_URIS, setOf())?.map { Uri.parse(it) }

            if (!imageUris.isNullOrEmpty()) {
                val newUri = if (RandomOrderController.isRandomOrderEnabled()) {
                    imageUris[random.nextInt(imageUris.size)]
                } else {
                    // Cycle through images sequentially
                    val currentIndex = imageUris.indexOf(currentUri)
                    val nextIndex = if (currentIndex == -1 || currentIndex == imageUris.size - 1) 0 else currentIndex + 1
                    imageUris[nextIndex]
                }

                currentUri = newUri

                // Load image using Glide
                Glide.with(applicationContext)
                    .load(newUri)
                    .override(1024, 1024) // Higher resolution
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            petImageView.setImageDrawable(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            petImageView.setImageDrawable(placeholder)
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            Log.e("PetService", "Failed to load image: $newUri")
                            petImageView.setImageDrawable(errorDrawable)
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e("PetService", "Error updating image: ${e.message}")
        }
    }

    private fun createPetView() {
        try {
            petView = LayoutInflater.from(this).inflate(R.layout.pet_layout, null)
            petImageView = petView.findViewById(R.id.petImageView)
            // Set initial scale type
            petImageView.scaleType = ImageView.ScaleType.FIT_CENTER

            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 100
            }

            setupTouchListener(params)

            try {
                windowManager.addView(petView, params)
                updatePetSize() // Apply size immediately after adding view
                handler.post { updatePetImage() }
            } catch (e: Exception) {
                Log.e("PetService", "Error adding view: ${e.message}")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("PetService", "Error creating pet view: ${e.message}")
            stopSelf()
        }
    }

    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        var initialX: Int = 0
        var initialY: Int = 0
        var initialTouchX: Float = 0f
        var initialTouchY: Float = 0f

        petView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()

                    // Keep pet within screen bounds
                    params.x = params.x.coerceIn(0, screenWidth - view.width)
                    params.y = params.y.coerceIn(0, screenHeight - view.height)

                    try {
                        windowManager.updateViewLayout(petView, params)
                    } catch (e: Exception) {
                        Log.e("PetService", "Error updating view layout: ${e.message}")
                    }
                    true
                }
                else -> false
            }
        }
    }

    private val imageUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.ACTION_IMAGES_UPDATED) {
                updatePetImage()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            setupScreenDimensions()
            createPetView()
            registerReceiver(imageUpdateReceiver, IntentFilter(Constants.ACTION_IMAGES_UPDATED))
            startRandomMovement()
            isViewCreated = true
            updatePetSize()

            // Start image update timer
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (RandomOrderController.isRandomOrderEnabled()) {
                        updatePetImage()
                    }
                    handler.postDelayed(this, ANIMATION_INTERVAL)
                }
            }, ANIMATION_INTERVAL)
        } catch (e: Exception) {
            Log.e("PetService", "Error in onCreate: ${e.message}")
            stopSelf()
        }

    }

    private fun setupScreenDimensions() {
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pet Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Desktop Pet Service"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Desktop Pet")
            .setContentText("Your pet is active")
            .setSmallIcon(R.drawable.ic_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startRandomMovement() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (MovementController.isMovementEnabled()) {
                    moveRandomly()
                }
                handler.postDelayed(this, ANIMATION_INTERVAL)
            }
        }, ANIMATION_INTERVAL)
    }

    private fun moveRandomly() {
        try {
            val params = petView.layoutParams as WindowManager.LayoutParams
            val newX = random.nextInt(screenWidth - petView.width)
            val newY = random.nextInt(screenHeight - petView.height)

            params.x = newX
            params.y = newY

            windowManager.updateViewLayout(petView, params)
        } catch (e: Exception) {
            Log.e("PetService", "Error in random movement: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isViewCreated) {
                try {

                    handler.removeCallbacksAndMessages(null)
                    windowManager.removeView(petView)
                    unregisterReceiver(imageUpdateReceiver)
                } catch (e: Exception) {
                    Log.e("PetService", "Error during service destruction", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PetService", "Error during service destruction", e)
        }

    }

    private fun updatePetSize() {
        try {
            val size = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(Constants.KEY_PET_SIZE, Constants.DEFAULT_SIZE)
                .coerceIn(Constants.MIN_SIZE, Constants.MAX_SIZE)

            // Convert dp to pixels
            val sizeInPixels = dpToPx(size)

            // Update window layout params
            val params = petView.layoutParams as WindowManager.LayoutParams
            params.width = sizeInPixels
            params.height = sizeInPixels

            // Update ImageView layout
            val imageParams = petImageView.layoutParams
            imageParams.width = sizeInPixels
            imageParams.height = sizeInPixels
            petImageView.layoutParams = imageParams

            // Apply scaleType
            petImageView.scaleType = ImageView.ScaleType.FIT_CENTER

            // Update the view in window manager
            if (isViewCreated) {
                try {
                    windowManager.updateViewLayout(petView, params)
                } catch (e: Exception) {
                    Log.e("PetService", "Error updating view layout: ${e.message}")
                }
            }

            Log.d("PetService", "Pet size updated to ${size}dp (${sizeInPixels}px)")
        } catch (e: Exception) {
            Log.e("PetService", "Error in updatePetSize: ${e.message}")
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
