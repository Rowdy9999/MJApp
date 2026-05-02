package com.mj.assistant

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.mj.assistant.databinding.ActivitySettingsBinding
import com.mj.assistant.util.AppConfig

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var keyVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentValues()
        buildPresetButtons()
        setupListeners()
    }

    private fun loadCurrentValues() {
        binding.etEndpoint.setText(AppConfig.apiUrl)
        binding.etApiKey.setText(AppConfig.apiKey)
        binding.etModel.setText(AppConfig.model)

        val temp = (AppConfig.temperature * 100).toInt()
        binding.seekTemperature.progress = temp
        binding.tvTempLabel.text = "Temperature: ${"%.1f".format(temp / 100f)}"
    }

    private fun buildPresetButtons() {
        binding.llPresets.removeAllViews()

        for (preset in AppConfig.allProviders) {
            val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = preset.name
                setTextColor(getColor(R.color.text_primary))
                strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.text_hint))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(com.google.android.material.R.dimen.mtrl_btn_default_btn)
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(com.google.android.material.R.dimen.mtrl_btn_text_btn_icon_padding)
                }
                setOnClickListener {
                    AppConfig.applyPreset(preset)
                    loadCurrentValues()
                    val keyHint = if (preset.needsKey) "Add your API key" else "No key needed"
                    Toast.makeText(this@SettingsActivity, "${preset.name} — $keyHint", Toast.LENGTH_SHORT).show()
                }
            }
            binding.llPresets.addView(btn)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Toggle API key visibility
        binding.btnToggleKey.setOnClickListener {
            keyVisible = !keyVisible
            binding.etApiKey.transformationMethod = if (keyVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            binding.btnToggleKey.setImageResource(
                if (keyVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
            )
            binding.etApiKey.setSelection(binding.etApiKey.text?.length ?: 0)
        }

        // Temperature slider
        binding.seekTemperature.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvTempLabel.text = "Temperature: ${"%.1f".format(progress / 100f)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Save
        binding.btnSave.setOnClickListener {
            val url = binding.etEndpoint.text.toString().trim()
            val key = binding.etApiKey.text.toString().trim()
            val model = binding.etModel.text.toString().trim()
            val temp = binding.seekTemperature.progress / 100f

            if (url.isEmpty()) {
                binding.etEndpoint.error = "Endpoint required"
                return@setOnClickListener
            }
            if (key.isEmpty()) {
                binding.etApiKey.error = "API key required"
                return@setOnClickListener
            }
            if (model.isEmpty()) {
                binding.etModel.error = "Model name required"
                return@setOnClickListener
            }

            AppConfig.apiUrl = url
            AppConfig.apiKey = key
            AppConfig.model = model
            AppConfig.temperature = temp

            binding.tvSaveStatus.text = "✓ Settings saved"
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }
    }
}
