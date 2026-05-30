package com.example.pdf

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _currentReadBlockId = MutableStateFlow<String?>(null)
    val currentReadBlockId = _currentReadBlockId.asStateFlow()

    private val _currentWordIndex = MutableStateFlow(0)
    val currentWordIndex = _currentWordIndex.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                setupProgressListeners()
            }
        }
    }

    private fun setupProgressListeners() {
        if (tts == null) return
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _currentReadBlockId.value = utteranceId
                _currentWordIndex.value = 0
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentReadBlockId.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentReadBlockId.value = null
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // Approximate word index being highlighted based on character start position
                val fullText = currentReadingText
                if (fullText != null && start <= fullText.length) {
                    val substring = fullText.substring(0, start)
                    val words = substring.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    _currentWordIndex.value = words.size
                }
            }
        })
    }

    private var currentReadingText: String? = null

    /**
     * Speaks the target TextBlock with a unique identifier
     */
    fun speak(blockId: String, text: String) {
        if (!isInitialized || tts == null) return
        
        stop()
        currentReadingText = text
        _currentReadBlockId.value = blockId
        _isSpeaking.value = true

        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, blockId)
        }
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, blockId)
    }

    fun stop() {
        if (isInitialized && tts != null) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _isSpeaking.value = false
        _currentReadBlockId.value = null
        currentReadingText = null
    }

    fun shutdown() {
        if (tts != null) {
            try {
                tts?.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        tts = null
    }
}
