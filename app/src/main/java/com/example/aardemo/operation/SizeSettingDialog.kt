package com.example.aardemo.operation

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import com.example.aardemo.Constants.MAX_SIZE
import com.example.aardemo.Constants.MIN_SIZE
import com.example.aardemo.R
import com.example.aardemo.databinding.DialogSizeSettingBinding

class SizeSettingDialog(
    context: Context,
    private val currentSize: Int,
    private val onSizeConfirmed: (Int) -> Unit
) : Dialog(context, R.style.RoundedDialog) {

    private lateinit var binding: DialogSizeSettingBinding
    private var selectedSize: Int = currentSize

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSizeSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDialog()
        setupViews()
    }

    private fun setupDialog() {
        window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupViews() {
        binding.apply {
            // Setup SeekBar
            sizeSeekBar.apply {
                min = MIN_SIZE
                max = MAX_SIZE
                progress = currentSize
            }

            updateSizeText(currentSize)

            sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedSize = progress
                    updateSizeText(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            cancelButton.setOnClickListener { dismiss() }
            confirmButton.setOnClickListener {
                onSizeConfirmed(selectedSize)
                dismiss()
            }
        }
    }

    private fun updateSizeText(size: Int) {
        binding.sizeValueText.text = "$size"
    }
}
