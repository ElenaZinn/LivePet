package com.example.aardemo.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.example.aardemo.operation.OperationActivity.Companion.PERMISSION_REQUEST_CODE


object PermissionNewUtils {

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestPermission(context: Activity) {
        when {
            // For Android 13 and above (API 33+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                requestPhotoPermissionApi33(context)
            }
            // For Android 10 and above (API 29+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                requestPhotoPermissionApi29(context)
            }
            // For Android 9 and below
            else -> {
                requestPhotoPermissionLegacy(context)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPhotoPermissionApi33(context: Activity) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                // do nothing
            }
            context.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                showPermissionRationaleDialog(Manifest.permission.READ_MEDIA_IMAGES, context)
            }
            else -> {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPhotoPermissionApi29(context: Activity) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // do nothing
            }
            context.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                showPermissionRationaleDialog(Manifest.permission.READ_EXTERNAL_STORAGE, context)
            }
            else -> {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPhotoPermissionLegacy(context: Activity) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // do nothing
            }
            context.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                showPermissionRationaleDialog(Manifest.permission.READ_EXTERNAL_STORAGE, context)
            }
            else -> {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun showPermissionRationaleDialog(permission: String, context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permission Required")
            .setMessage("This app needs access to your photos to allow you to select images.")
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(permission),
                    PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showToast("Permission is required to select images", context)
            }
            .show()
    }

    private fun showToast(message: String, context: Activity) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}