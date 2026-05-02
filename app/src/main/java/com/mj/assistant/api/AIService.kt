package com.mj.assistant.api

import com.mj.assistant.util.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
You are MJ, a smart AI assistant inside an Android phone.
You MUST ALWAYS reply in the following EXACT format (no extra text outside this format):

action: <action_name>
param1: <value>
param2: <value>
response: <hinglish reply>

Available actions:
- youtube_search  → param1 = search query, param2 = unused
- open_app        → param1 = app name (e.g. whatsapp, chrome, settings, camera, gallery, maps), param2 = unused
- system_time     → param1 = unused, param2 = unused (respond with current time in response)
- system_torch    → param1 = on/off/toggle, param2 = unused
- chat            → param1 = unused, param2 = unused (just conversation)

Rules:
- response must be a short, friendly Hinglish sentence (mix of Hindi + English)
- If user intent is ambiguous, use action: chat
- Never break the format. No markdown, no code blocks, no extra explanation.
- param1 and param2 can be "unused" if not needed.

Example:
User: search funny cat videos on youtube
action: youtube_search
param1: funny cat videos
param2: unused
response: YouTube pe funny cat videos dhundh raha hoon, maza aayega!

Example:
User: what time is it
action: system_time
param1: unused
param2: unused
response: Abhi time dekh raha hoon...
""".trimIndent()

    suspend fun ask(userMessage: String): String = withContext(Dispatchers.IO) {
        val apiUrl = AppConfig.apiUrl
        val apiKey = AppConfig.apiKey
        val model = AppConfig.model
        val temperature = AppConfig.temperature.toDouble()

        if (apiKey.isBlank()) {
            throw Exception("API key not configured. Go to Settings → set your API key.")
        }

        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            put(JSONObject().put("role", "user").put("content", userMessage))
        }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("temperature", temperature)
            put("max_tokens", 300)
        }

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()?.take(200) ?: ""
            throw Exception("API ${response.code}: ${response.message}. $errorBody")
        }

        val json = JSONObject(response.body?.string() ?: throw Exception("Empty response"))
        val choices = json.getJSONArray("choices")
        if (choices.length() == 0) throw Exception("No response from model")

        choices.getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}
