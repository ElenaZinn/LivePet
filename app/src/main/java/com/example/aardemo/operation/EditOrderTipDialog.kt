package com.example.aardemo.operation

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.aardemo.R
import com.example.aardemo.databinding.DialogEditOrderTipBinding

class EditOrderTipDialog(
    context: Context,
) : Dialog(context, R.style.RoundedDialog) {

    private lateinit var binding: DialogEditOrderTipBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogEditOrderTipBinding.inflate(layoutInflater)
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

            confirmButton.setOnClickListener {
                dismiss()
            }
        }
    }

}
