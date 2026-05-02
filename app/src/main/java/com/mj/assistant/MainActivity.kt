package com.mj.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mj.assistant.action.ActionHandler
import com.mj.assistant.api.AIService
import com.mj.assistant.databinding.ActivityMainBinding
import com.mj.assistant.parser.ResponseParser
import com.mj.assistant.ui.Message
import com.mj.assistant.ui.MessageAdapter
import com.mj.assistant.util.AppConfig
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var actionHandler: ActionHandler

    private val aiService = AIService()
    private val messages = mutableListOf<Message>()
    private var isListening = false
    private var ttsReady = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init config (must be before anything reads it)
        AppConfig.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actionHandler = ActionHandler(this)
        tts = TextToSpeech(this, this)

        setupRecyclerView()
        setupSpeechRecognizer()
        setupClickListeners()

        // Greeting — warn if not configured
        if (!AppConfig.isConfigured) {
            addMjMessage("Hey! I'm MJ. ⚙️ You need to set your API key first. Tap the gear icon to open Settings.")
        } else {
            addMjMessage("Hey! I'm MJ, your assistant. Tap the mic or type something.")
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-init speech recognizer each time (can fail if Activity was recycled)
        setupSpeechRecognizer()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        if (::speechRecognizer.isInitialized) {
            try { speechRecognizer.destroy() } catch (_: Exception) {}
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.tvStatus.text = getString(R.string.listening)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                binding.tvStatus.text = "processing…"
            }
            override fun onError(error: Int) {
                setListeningState(false)
                binding.tvStatus.text = "online"
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error."
                    else -> "Speech error ($error)"
                }
                addMjMessage(msg)
            }
            override fun onResults(results: Bundle?) {
                setListeningState(false)
                binding.tvStatus.text = "online"
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) handleUserInput(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupClickListeners() {
        // Settings gear
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Mic button
        binding.btnMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE
                )
                return@setOnClickListener
            }
            if (!AppConfig.isConfigured) {
                addMjMessage("⚙️ Set your API key first. Tap the gear icon above.")
                return@setOnClickListener
            }
            if (isListening) stopListening() else startListening()
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.etInput.text?.clear()
                if (!AppConfig.isConfigured) {
                    addMjMessage("⚙️ Set your API key first. Tap the gear icon above.")
                    return@setOnClickListener
                }
                handleUserInput(text)
            }
        }

        // Keyboard send action
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.btnSend.performClick()
                true
            } else false
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
        setListeningState(true)
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        setListeningState(false)
    }

    private fun setListeningState(listening: Boolean) {
        isListening = listening
        binding.vMicGlow.visibility = if (listening) View.VISIBLE else View.GONE
        binding.tvStatus.text = if (listening) getString(R.string.listening) else "online"
    }

    private fun handleUserInput(text: String) {
        addUserMessage(text)
        binding.tvStatus.text = getString(R.string.thinking)

        lifecycleScope.launch {
            try {
                val raw = aiService.ask(text)
                val parsed = ResponseParser.parse(raw)
                addMjMessage(parsed.response)
                speak(parsed.response)
                if (parsed.action != "chat") {
                    actionHandler.execute(parsed.action, parsed.param1, parsed.param2)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: getString(R.string.error_api)
                addMjMessage("❌ $errorMsg")
                speak("Sorry, something went wrong.")
            } finally {
                binding.tvStatus.text = "online"
            }
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(Message(text, isUser = true))
        messageAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun addMjMessage(text: String) {
        messages.add(Message(text, isUser = false))
        messageAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        val clean = text.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "").trim()
        if (clean.isEmpty()) return
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "mj_utterance")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(1.0f)
            ttsReady = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
    }
}
