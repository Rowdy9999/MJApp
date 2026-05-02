# MJ — Android AI Assistant

A voice-powered AI assistant for Android. Tap the mic, speak, and MJ responds — then takes action.

## What It Does

| You Say | MJ Does |
|---------|---------|
| "Search funny cats on YouTube" | Opens YouTube with search results |
| "Open WhatsApp" | Launches WhatsApp |
| "What time is it?" | Speaks the current time |
| "Turn on flashlight" | Toggles the torch |
| "Tell me a joke" | Chat response + speaks it |

## Architecture

```
User Voice → SpeechRecognizer → Text
                                    ↓
                              AIService (API POST)
                                    ↓
                              ResponseParser
                                    ↓
                    ┌───────────────┼───────────────┐
                    ↓               ↓               ↓
               ActionHandler    Display        TTS Speak
              (open/torch/     (RecyclerView)  (response)
               YouTube/time)
```

## Setup

### 1. Configure in App

Launch the app → tap the **⚙️ gear icon** (top right) → Settings screen:

| Field | What to Set |
|-------|-------------|
| **API Endpoint** | Your provider's chat completions URL |
| **API Key** | Your secret key |
| **Model** | Model name (e.g. `gpt-4o-mini`, `llama3`) |

**22 providers built in** — tap any to auto-fill endpoint + default model:

| Category | Providers |
|----------|-----------|
| **Major Cloud** | OpenAI, Anthropic (Claude), Google Gemini, Mistral, DeepSeek, Cohere, Perplexity |
| **Aggregator** | OpenRouter |
| **Fast Inference** | Groq, Together AI, Fireworks AI |
| **Xiaomi** | Xiaomi MiMo |
| **Open-Source** | Hugging Face, Replicate |
| **Cloud AI** | Azure OpenAI, AWS Bedrock |
| **Local / Self-Hosted** | Ollama, LM Studio, vLLM, Text Generation WebUI, Jan |

All settings are saved locally via SharedPreferences — no data leaves the device except the API call itself.

### 2. Build the APK

**Option A — Android Studio:**
1. Open the `MJApp` folder in Android Studio
2. Sync Gradle
3. Build → Build APK

**Option B — Command Line:**
```bash
cd MJApp
chmod +x build.sh
./build.sh debug
```

### 3. Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
MJApp/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/mj/assistant/
│   │   ├── MainActivity.kt            ← Chat UI, voice, TTS
│   │   ├── SettingsActivity.kt        ← API config screen
│   │   ├── api/AIService.kt           ← HTTP → AI API (reads from AppConfig)
│   │   ├── parser/ResponseParser.kt
│   │   ├── action/ActionHandler.kt
│   │   ├── ui/
│   │   │   ├── Message.kt
│   │   │   └── MessageAdapter.kt
│   │   └── util/AppConfig.kt          ← SharedPreferences persistence
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml      ← Chat + gear icon
│       │   ├── activity_settings.xml  ← Config form + presets
│       │   └── item_message.xml
│       ├── drawable/                  ← Icons, bubbles, glow
│       └── values/
├── build.gradle.kts
├── app/build.gradle.kts
└── build.sh
```

## AI Response Format

MJ's AI must always return this exact structure:

```
action: youtube_search
param1: funny cat videos
param2: unused
response: YouTube pe search kar raha hoon!
```

The system prompt in `AIService.kt` enforces this. You can customize it.

## Customization

- **Theme colors** → `res/values/colors.xml`
- **AI personality** → system prompt in `api/AIService.kt`
- **App mapping** → `openApp()` in `action/ActionHandler.kt`
- **Speech rate** → `tts.setSpeechRate()` in `MainActivity.kt`
- **API provider** → changeable live in Settings (⚙️) — no rebuild needed

## Requirements

- Android 8.0+ (API 26)
- Internet connection (for AI API)
- Microphone (for voice input)
- Camera flash (optional, for torch)

## Permissions

| Permission | Why |
|-----------|-----|
| `RECORD_AUDIO` | Voice input via SpeechRecognizer |
| `INTERNET` | AI API calls |
| `CAMERA` | Flashlight toggle |
