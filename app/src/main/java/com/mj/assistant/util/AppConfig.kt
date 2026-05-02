package com.mj.assistant.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton that persists AI provider config via SharedPreferences.
 */
object AppConfig {

    private const val PREFS_NAME = "mj_config"
    private const val KEY_API_URL = "api_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_TEMPERATURE = "temperature"

    private const val DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions"
    private const val DEFAULT_API_KEY = ""
    private const val DEFAULT_MODEL = "gpt-4o-mini"
    private const val DEFAULT_TEMPERATURE = 0.7f

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var apiUrl: String
        get() = prefs.getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
        set(value) = prefs.edit().putString(KEY_API_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    // ═══════════════════════════════════════════════════════════
    //  PROVIDER PRESETS
    // ═══════════════════════════════════════════════════════════

    data class ProviderPreset(
        val name: String,
        val endpoint: String,
        val defaultModel: String,
        val needsKey: Boolean = true
    )

    val allProviders = listOf(

        // ── Major Cloud Providers ──────────────────────────────
        ProviderPreset(
            "OpenAI",
            "https://api.openai.com/v1/chat/completions",
            "gpt-4o-mini"
        ),
        ProviderPreset(
            "Anthropic (Claude)",
            "https://api.anthropic.com/v1/messages",
            "claude-sonnet-4-20250514"
        ),
        ProviderPreset(
            "Google Gemini",
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            "gemini-2.0-flash"
        ),
        ProviderPreset(
            "Mistral",
            "https://api.mistral.ai/v1/chat/completions",
            "mistral-small-latest"
        ),
        ProviderPreset(
            "DeepSeek",
            "https://api.deepseek.com/v1/chat/completions",
            "deepseek-chat"
        ),
        ProviderPreset(
            "Cohere",
            "https://api.cohere.com/v2/chat",
            "command-r-plus"
        ),
        ProviderPreset(
            "Perplexity",
            "https://api.perplexity.ai/chat/completions",
            "sonar-pro"
        ),

        // ── Aggregators / Routers ──────────────────────────────
        ProviderPreset(
            "OpenRouter",
            "https://openrouter.ai/api/v1/chat/completions",
            "openai/gpt-4o-mini"
        ),

        // ── High-Performance Inference ─────────────────────────
        ProviderPreset(
            "Groq",
            "https://api.groq.com/openai/v1/chat/completions",
            "llama-3.3-70b-versatile"
        ),
        ProviderPreset(
            "Together AI",
            "https://api.together.xyz/v1/chat/completions",
            "meta-llama/Llama-3-70b-chat-hf"
        ),
        ProviderPreset(
            "Fireworks AI",
            "https://api.fireworks.ai/inference/v1/chat/completions",
            "accounts/fireworks/models/llama-v3p1-70b-instruct"
        ),

        // ── Xiaomi MiMo ───────────────────────────────────────
        ProviderPreset(
            "Xiaomi MiMo",
            "https://api.xiaomi.com/v1/chat/completions",
            "MiMo-v2.5-Pro"
        ),

        // ── Open-Source / Hugging Face ─────────────────────────
        ProviderPreset(
            "Hugging Face",
            "https://api-inference.huggingface.co/models/",
            "meta-llama/Llama-3.3-70B-Instruct"
        ),
        ProviderPreset(
            "Replicate",
            "https://api.replicate.com/v1/predictions",
            "meta/llama-3-70b-instruct"
        ),

        // ── Cloud Platform AI ──────────────────────────────────
        ProviderPreset(
            "Azure OpenAI",
            "https://YOUR_RESOURCE.openai.azure.com/openai/deployments/YOUR_DEPLOYMENT/chat/completions?api-version=2024-08-01-preview",
            "gpt-4o-mini"
        ),
        ProviderPreset(
            "AWS Bedrock",
            "https://bedrock-runtime.us-east-1.amazonaws.com/model/anthropic.claude-3-sonnet-20240229-v1:0/invoke",
            "anthropic.claude-3-sonnet-20240229-v1:0"
        ),

        // ── Local / Self-Hosted ────────────────────────────────
        ProviderPreset(
            "Ollama (Local)",
            "http://localhost:11434/v1/chat/completions",
            "llama3",
            needsKey = false
        ),
        ProviderPreset(
            "LM Studio (Local)",
            "http://localhost:1234/v1/chat/completions",
            "local-model",
            needsKey = false
        ),
        ProviderPreset(
            "vLLM (Local)",
            "http://localhost:8000/v1/chat/completions",
            "default",
            needsKey = false
        ),
        ProviderPreset(
            "Text Generation WebUI",
            "http://localhost:5000/v1/chat/completions",
            "default",
            needsKey = false
        ),
        ProviderPreset(
            "Jan (Local)",
            "http://localhost:1337/v1/chat/completions",
            "default",
            needsKey = false
        )
    )

    fun applyPreset(preset: ProviderPreset) {
        apiUrl = preset.endpoint
        model = preset.defaultModel
        if (!preset.needsKey) {
            apiKey = "not-needed"
        }
    }
}
