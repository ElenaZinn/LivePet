package com.example.aardemo

import androidx.annotation.RequiresApi
import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.aardemo.databinding.ActivityMainBinding
import com.example.aardemo.utils.MovementController
import com.example.aardemo.operation.OperationActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var petService: Intent? = null

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
        private const val OPERATION_REQUEST_CODE = 1235
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initializeViews()
        checkPermissions()
        updateServiceStatus()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initializeViews() {
        binding.apply {
            startPetButton.text = "${getString(R.string.pet_launch)} ${getString(R.string.app_name)}"
            stopPetButton.text = "${getString(R.string.pet_stop)} ${getString(R.string.app_name)}"

            startPetButton.setOnClickListener {
                if (checkOverlayPermission()) {
                    startPetService()
                }
            }

            stopPetButton.setOnClickListener {
                stopPetService()
            }

            settingsButton.setOnClickListener {
                openSettings()
                stopPetService()
            }

            toggleMovementButton.setOnClickListener {
                toggleRandomMovement()
            }
        }

        updateMovementButtonState()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        for (permission in permissions) {
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, 1)
                return
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startPetService() {
        try {
            if (!Settings.canDrawOverlays(this)) {
                showToast("Overlay permission not granted")
                requestOverlayPermission()
                return
            }

            petService = Intent(this, PetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(petService)
            } else {
                startService(petService)
            }

            updateServiceStatus(true)
//            showToast("Desktop pet started")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting service: ${e.message}")
            showToast("Error: ${e.message}")
        }
    }

    private fun stopPetService() {
        try {
            petService?.let {
                stopService(it)
                updateServiceStatus(false)
//                showToast("Desktop pet stopped")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping service: ${e.message}")
            showToast("Error stopping service")
        }
    }

    private fun updateServiceStatus(isRunning: Boolean = isServiceRunning()) {
        binding.apply {
            startPetButton.isEnabled = !isRunning
            stopPetButton.isEnabled = isRunning
            toggleMovementButton.isEnabled = isRunning
            serviceStatusText.text = if (isRunning) {
                "${getString(R.string.app_name)} ${getString(R.string.pet_running)}"
            } else {
                "${getString(R.string.app_name)} ${getString(R.string.pet_stopped)}"
            }
        }
        updateMovementButtonState()
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == PetService::class.java.name }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkOverlayPermission(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return false
        }
        return true
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    private fun openSettings() {
        val intent = Intent(this, OperationActivity::class.java)
        startActivityForResult(intent, OPERATION_REQUEST_CODE)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPermissionDeniedDialog(permission: String) {
        val message = when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES -> {
                "Storage permission is required to select images for your desktop pet"
            }
            else -> "This permission is required for the app to function properly"
        }

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                checkPermissions()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Settings.canDrawOverlays(this)) {
                    this.startPetService()
                } else {
                    showToast("Overlay permission is required")
                }
            }
            OPERATION_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    if (isServiceRunning()) {
                        stopPetService()
                        startPetService()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Permission granted")
        } else {
            showPermissionDeniedDialog(permissions[0])
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun toggleRandomMovement() {
        val isEnabled = MovementController.toggleMovement()
        updateMovementButtonState()
        showToast(if (isEnabled) "Random movement enabled" else "Random movement disabled")
    }

    private fun updateMovementButtonState() {
        binding.toggleMovementButton.apply {
            val isEnabled = MovementController.isMovementEnabled()
            setBackgroundColor(if (isEnabled)
                resources.getColor(android.R.color.holo_blue_light)
            else resources.getColor(android.R.color.darker_gray)
            )
            text = if (isEnabled) "Disable Random Movement" else "Enable Random Movement"
        }
    }



}
