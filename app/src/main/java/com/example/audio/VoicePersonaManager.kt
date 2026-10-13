package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import com.example.network.GeminiPoolService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages audio speech generation and playback using the dedicated Gemini TTS model (gemini-3.8-flash-lite-tts).
 * Directly synthesizes authentic AI voice waveforms for each of the 20 distinct personas,
 * with local caching for instantaneous preview replay and zero wasted API quota.
 */
class VoicePersonaManager(
    private val context: Context,
    private val poolService: GeminiPoolService
) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSynthesisJob: Job? = null

    private val _currentlySpeakingPersonaId = MutableStateFlow<String?>(null)
    val currentlySpeakingPersonaId: StateFlow<String?> = _currentlySpeakingPersonaId.asStateFlow()

    private val _isLoadingPersonaId = MutableStateFlow<String?>(null)
    val isLoadingPersonaId: StateFlow<String?> = _isLoadingPersonaId.asStateFlow()

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    val currentlySpeakingMessageId: StateFlow<String?> = _currentlySpeakingMessageId.asStateFlow()

    init {
        // Clear any old legacy cache files containing preamble meta-text
        try {
            val cacheFiles = context.cacheDir.listFiles()
            cacheFiles?.forEach { file ->
                if (file.name.startsWith("persona_audio_") || file.name.startsWith("gemini_tts_")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Previews a voice persona by synthesizing or playing ONLY its sample text quote.
     * Uses the authentic Gemini model (gemini-3.8-flash-lite-tts).
     * Cached after first fetch for instant playback on subsequent taps.
     */
    fun previewPersona(
        persona: VoicePersona,
        model: String = "gemini-3.8-flash-lite-tts",
        scope: CoroutineScope
    ) {
        stop()

        val safeModel = model.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val cacheFile = File(context.cacheDir, "persona_v2_${persona.id}_${safeModel}.wav")

        // Instant play if already cached
        if (cacheFile.exists() && cacheFile.length() > 500) {
            _currentlySpeakingPersonaId.value = persona.id
            playAudioFile(cacheFile, persona.id)
            return
        }

        // Fetch from Gemini TTS model
        _currentlySpeakingPersonaId.value = persona.id
        _isLoadingPersonaId.value = persona.id

        currentSynthesisJob = scope.launch(Dispatchers.IO) {
            val result = poolService.synthesizeSpeech(
                text = persona.sampleText,
                persona = persona,
                model = model
            )

            withContext(Dispatchers.Main) {
                _isLoadingPersonaId.value = null
                if (result.isSuccess) {
                    val bytes = result.getOrNull()
                    if (bytes != null && bytes.isNotEmpty()) {
                        try {
                            cacheFile.writeBytes(bytes)
                            playAudioFile(cacheFile, persona.id)
                        } catch (e: Exception) {
                            Log.e("VoicePersonaManager", "Failed to write or play cached audio: ${e.message}")
                            _currentlySpeakingPersonaId.value = null
                        }
                    } else {
                        _currentlySpeakingPersonaId.value = null
                    }
                } else {
                    _currentlySpeakingPersonaId.value = null
                    val err = result.exceptionOrNull()?.message ?: "Synthesis failed"
                    Log.w("VoicePersonaManager", "Gemini TTS synthesis failed: $err")
                    Toast.makeText(context, "Gemini TTS: $err", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Synthesizes and speaks arbitrary text with the chosen AI persona using Gemini TTS.
     */
    fun speakText(
        text: String,
        persona: VoicePersona,
        model: String = "gemini-3.8-flash-lite-tts",
        scope: CoroutineScope,
        messageId: String? = null
    ) {
        if (text.isBlank()) return

        // If already speaking this message, toggle stop
        if (messageId != null && _currentlySpeakingMessageId.value == messageId) {
            stop()
            return
        }

        stop()

        _currentlySpeakingPersonaId.value = persona.id
        _isLoadingPersonaId.value = persona.id
        _currentlySpeakingMessageId.value = messageId

        currentSynthesisJob = scope.launch(Dispatchers.IO) {
            val result = poolService.synthesizeSpeech(
                text = text,
                persona = persona,
                model = model
            )

            withContext(Dispatchers.Main) {
                _isLoadingPersonaId.value = null
                if (result.isSuccess) {
                    val bytes = result.getOrNull()
                    if (bytes != null && bytes.isNotEmpty()) {
                        try {
                            val tempFile = File.createTempFile("gemini_speech_", ".wav", context.cacheDir)
                            tempFile.writeBytes(bytes)
                            tempFile.deleteOnExit()
                            playAudioFile(tempFile, persona.id, messageId)
                        } catch (e: Exception) {
                            Log.e("VoicePersonaManager", "Error playing response audio: ${e.message}")
                            _currentlySpeakingPersonaId.value = null
                            _currentlySpeakingMessageId.value = null
                        }
                    } else {
                        _currentlySpeakingPersonaId.value = null
                        _currentlySpeakingMessageId.value = null
                    }
                } else {
                    _currentlySpeakingPersonaId.value = null
                    _currentlySpeakingMessageId.value = null
                    val err = result.exceptionOrNull()?.message ?: "Synthesis error"
                    Log.w("VoicePersonaManager", "Gemini speakText error: $err")
                }
            }
        }
    }

    private fun playAudioFile(file: File, personaId: String, messageId: String? = null) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    if (_currentlySpeakingPersonaId.value == personaId) {
                        _currentlySpeakingPersonaId.value = null
                    }
                    if (_currentlySpeakingMessageId.value == messageId) {
                        _currentlySpeakingMessageId.value = null
                    }
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    if (_currentlySpeakingPersonaId.value == personaId) {
                        _currentlySpeakingPersonaId.value = null
                    }
                    if (_currentlySpeakingMessageId.value == messageId) {
                        _currentlySpeakingMessageId.value = null
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("VoicePersonaManager", "MediaPlayer playback failed: ${e.message}")
            _currentlySpeakingPersonaId.value = null
            _currentlySpeakingMessageId.value = null
        }
    }

    /**
     * Stops active speech playback or pending synthesis.
     */
    fun stop() {
        currentSynthesisJob?.cancel()
        currentSynthesisJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("VoicePersonaManager", "Error stopping MediaPlayer: ${e.message}")
        }
        mediaPlayer = null
        _currentlySpeakingPersonaId.value = null
        _isLoadingPersonaId.value = null
        _currentlySpeakingMessageId.value = null
    }

    fun shutdown() {
        stop()
    }
}
