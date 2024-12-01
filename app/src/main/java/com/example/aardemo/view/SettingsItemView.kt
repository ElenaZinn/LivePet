package com.example.aardemo.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.aardemo.R
import com.example.aardemo.databinding.ViewSettingsItemBinding

// SettingsItemView.kt
class SettingsItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewSettingsItemBinding

    private var title: String = ""
        set(value) {
            field = value
            binding.titleText.text = value
        }

    private var description: String = ""
        set(value) {
            field = value
            binding.descriptionText.text = value
            binding.descriptionText.visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
        }

    private var isToggleEnabled: Boolean = false
        set(value) {
            field = value
            binding.toggleSwitch.isChecked = value
        }

    init {
        binding = ViewSettingsItemBinding.inflate(LayoutInflater.from(context), this)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SettingsItemView,
            0, 0
        ).apply {
            try {
                title = getString(R.styleable.SettingsItemView_settingTitle) ?: ""
                description = getString(R.styleable.SettingsItemView_settingDescription) ?: ""
                isToggleEnabled = getBoolean(R.styleable.SettingsItemView_settingEnabled, false)
            } finally {
                recycle()
            }
        }

        setupToggleAnimation()
    }

    private fun setupToggleAnimation() {
        binding.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            AnimatorSet().apply {
                val scale = if (isChecked) 1.2f else 1.0f
                play(ObjectAnimator.ofFloat(binding.toggleSwitch, "scaleX", scale))
                    .with(ObjectAnimator.ofFloat(binding.toggleSwitch, "scaleY", scale))
                duration = 150
                interpolator = OvershootInterpolator()
                start()
            }
        }
    }

    fun setOnToggleChangedListener(listener: (Boolean) -> Unit) {
        binding.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            listener(isChecked)
        }
    }
}
