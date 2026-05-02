package com.mj.assistant.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionHandler(private val context: Context) {

    private var torchEnabled = false

    fun execute(action: String, param1: String, param2: String) {
        when (action) {
            "youtube_search" -> youtubeSearch(param1)
            "open_app" -> openApp(param1)
            "system_time" -> showTime()
            "system_torch" -> toggleTorch(param1)
            "chat" -> { /* already handled by response display */ }
            else -> { /* unknown action, ignore */ }
        }
    }

    private fun youtubeSearch(query: String) {
        if (query.isBlank()) return
        try {
            // Try YouTube app first
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (isIntentSafe(appIntent)) {
                context.startActivity(appIntent)
                return
            }
            // Fallback to browser
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't open YouTube", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openApp(appName: String) {
        if (appName.isBlank()) return
        val normalized = appName.lowercase().trim()

        val packageName = when {
            normalized.contains("whatsapp") -> "com.whatsapp"
            normalized.contains("chrome") -> "com.android.chrome"
            normalized.contains("camera") -> "com.android.camera"
            normalized.contains("gallery") -> "com.google.android.apps.photos"
            normalized.contains("maps") -> "com.google.android.apps.maps"
            normalized.contains("settings") -> "com.android.settings"
            normalized.contains("phone") || normalized.contains("dialer") -> "com.google.android.dialer"
            normalized.contains("messages") || normalized.contains("sms") -> "com.google.android.apps.messaging"
            normalized.contains("gmail") || normalized.contains("mail") -> "com.google.android.gm"
            normalized.contains("clock") || normalized.contains("alarm") -> "com.google.android.deskclock"
            normalized.contains("calculator") -> "com.google.android.calculator"
            normalized.contains("play store") -> "com.android.vending"
            normalized.contains("files") -> "com.google.android.apps.nbu.files"
            normalized.contains("contacts") -> "com.google.android.contacts"
            normalized.contains("calendar") -> "com.google.android.calendar"
            normalized.contains("spotify") -> "com.spotify.music"
            normalized.contains("instagram") -> "com.instagram.android"
            normalized.contains("twitter") || normalized.contains("x") -> "com.twitter.android"
            normalized.contains("facebook") -> "com.facebook.katana"
            normalized.contains("netflix") -> "com.netflix.mediaclient"
            else -> null
        }

        try {
            if (packageName != null) {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            }
            // Fallback: search in Play Store
            val playIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/search?q=${Uri.encode(appName)}&c=apps")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't open $appName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTime() {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = sdf.format(Date())
        Toast.makeText(context, "⏰ $time", Toast.LENGTH_LONG).show()
    }

    private fun toggleTorch(param: String) {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Toast.makeText(context, "No flashlight found", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return

            when {
                param.equals("on", ignoreCase = true) -> {
                    cameraManager.setTorchMode(cameraId, true)
                    torchEnabled = true
                }
                param.equals("off", ignoreCase = true) -> {
                    cameraManager.setTorchMode(cameraId, false)
                    torchEnabled = false
                }
                else -> { // toggle
                    torchEnabled = !torchEnabled
                    cameraManager.setTorchMode(cameraId, torchEnabled)
                }
            }
        } catch (e: CameraAccessException) {
            Toast.makeText(context, "Flashlight error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isIntentSafe(intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }
}
