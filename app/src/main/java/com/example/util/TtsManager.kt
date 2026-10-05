package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.data.storage.UserProgressManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class TtsManager(
    private val context: Context,
    private val apiKeyProvider: (() -> String)? = null
) : TextToSpeech.OnInitListener {

    private val applicationContext: Context = context.applicationContext

    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set
    var isGermanSupported: Boolean = false
        private set

    // CHANGE 8: Gemini TTS HTTP client & cache
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioCacheDir = File(applicationContext.cacheDir, "gemini_tts_cache").apply { mkdirs() }
    private var mediaPlayer: MediaPlayer? = null

    init {
        tts = createTts()
    }

    // CHANGE 7: Initialize TextToSpeech preferring Google TTS engine package
    private fun createTts(): TextToSpeech {
        val googleEngine = "com.google.android.tts"
        val isGoogleInstalled = try {
            applicationContext.packageManager.getPackageInfo(googleEngine, 0)
            true
        } catch (e: Exception) {
            false
        }

        return if (isGoogleInstalled) {
            try {
                TextToSpeech(applicationContext, this, googleEngine)
            } catch (e: Exception) {
                TextToSpeech(applicationContext, this)
            }
        } else {
            TextToSpeech(applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.GERMAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "German language is missing or not supported on this device.")
                isGermanSupported = false
                isInitialized = true
            } else {
                isGermanSupported = true
                isInitialized = true
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)

                // CHANGE 7: Select best installed de-DE voice (offline and high quality preferred)
                try {
                    val voices = tts?.voices
                    if (!voices.isNullOrEmpty()) {
                        val germanVoices = voices.filter { voice ->
                            voice.locale.language.equals("de", ignoreCase = true)
                        }
                        val bestVoice = germanVoices
                            .filter { it.locale.country.equals("DE", ignoreCase = true) }
                            .sortedWith(
                                compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                                    .thenByDescending { it.quality }
                            )
                            .firstOrNull() ?: germanVoices.firstOrNull()

                        if (bestVoice != null) {
                            tts?.voice = bestVoice
                            Log.d("TtsManager", "Selected German voice: ${bestVoice.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("TtsManager", "Could not set custom voice: ${e.message}")
                }
            }
        } else {
            Log.e("TtsManager", "TTS initialization failed with code $status")
            isInitialized = false
            isGermanSupported = false
        }
    }

    fun speak(text: String, slow: Boolean = false) {
        if (text.isBlank()) return

        // Clean German text (remove extra punctuation or Persian script if mixed)
        val cleanGerman = text.replace(Regex("[\\u0600-\\u06FF]"), "").trim()
        val speechText = if (cleanGerman.isNotEmpty()) cleanGerman else text

        val apiKey = apiKeyProvider?.invoke()?.trim()
            ?: UserProgressManager.getInstance(applicationContext).getEffectiveGeminiApiKey().trim()

        if (apiKey.isNotEmpty()) {
            // CHANGE 8: Gemini TTS is primary when API key is present
            coroutineScope.launch {
                val success = playGeminiTts(speechText, slow, apiKey)
                if (!success) {
                    // Silent fallback to device TTS (no error shown to the user)
                    speakWithDeviceTts(speechText, slow)
                }
            }
        } else {
            // Fallback directly to device TTS
            speakWithDeviceTts(speechText, slow)
        }
    }

    // CHANGE 8: Gemini TTS request with on-device file caching
    private suspend fun playGeminiTts(text: String, slow: Boolean, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = getCacheKey(text, slow)
                val cachedFile = File(audioCacheDir, "$cacheKey.mp3")

                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return@withContext playAudioFile(cachedFile)
                }

                // The SLOW button requests slow, clear delivery via TTS prompt instruction
                val promptText = if (slow) {
                    "Speak this German phrase slowly, clearly, and carefully for an A1 language learner, pronouncing every syllable distinctly: \"$text\""
                } else {
                    "Pronounce this German text clearly and naturally with standard German (Hochdeutsch) pronunciation: \"$text\""
                }

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", promptText)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", "Kore")
                                })
                            })
                        })
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"
                val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w("TtsManager", "Gemini TTS HTTP error: ${response.code}")
                    return@withContext false
                }

                val responseBodyStr = response.body?.string().orEmpty()
                if (responseBodyStr.isBlank()) return@withContext false

                val respObj = JSONObject(responseBodyStr)
                val candidates = respObj.optJSONArray("candidates") ?: return@withContext false
                if (candidates.length() == 0) return@withContext false

                val firstCand = candidates.getJSONObject(0)
                val content = firstCand.optJSONObject("content") ?: return@withContext false
                val parts = content.optJSONArray("parts") ?: return@withContext false
                if (parts.length() == 0) return@withContext false

                var audioBase64: String? = null
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    val inlineData = p.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val data = inlineData.optString("data", "")
                        if (data.isNotBlank()) {
                            audioBase64 = data
                            break
                        }
                    }
                }

                if (audioBase64.isNullOrBlank()) return@withContext false

                val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                if (audioBytes.isEmpty()) return@withContext false

                cachedFile.outputStream().use { it.write(audioBytes) }

                return@withContext playAudioFile(cachedFile)
            } catch (e: Exception) {
                Log.w("TtsManager", "Gemini TTS failed silently: ${e.message}")
                false
            }
        }
    }

    private suspend fun playAudioFile(file: File): Boolean = withContext(Dispatchers.Main) {
        try {
            stopAudio()
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.prepare()
            player.start()
            player.setOnCompletionListener { mp ->
                try {
                    mp.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                try {
                    mp.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
                true
            }
            mediaPlayer = player
            true
        } catch (e: Exception) {
            Log.w("TtsManager", "MediaPlayer playback failed: ${e.message}")
            false
        }
    }

    private fun speakWithDeviceTts(speechText: String, slow: Boolean) {
        stopAudio()
        if (!isInitialized) {
            showToast("موتور صوتی گوشی هنوز آماده نشده است.")
            return
        }

        if (!isGermanSupported) {
            showToast("بسته صدای آلمانی روی گوشی شما نصب نیست. لطفاً در تنظیمات زبان گوشی (Text-to-Speech) صدای آلمانی را فعال کنید.")
            return
        }

        // Set speech rate immediately before every speak call: 1.0f for normal, 0.55f for slow
        val speechRate = if (slow) 0.55f else 1.0f
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(1.0f)

        // Pass utterance Bundle with KEY_PARAM_VOLUME set to 1.0f
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "german_tts_${System.currentTimeMillis()}")
    }

    private fun getCacheKey(text: String, isSlow: Boolean): String {
        val speedTag = if (isSlow) "slow" else "normal"
        val raw = "${text.trim()}_$speedTag"
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    fun stop() {
        stopAudio()
        tts?.stop()
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        coroutineScope.cancel()
    }
}
