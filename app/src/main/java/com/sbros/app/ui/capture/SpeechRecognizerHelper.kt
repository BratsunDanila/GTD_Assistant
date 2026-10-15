package com.sbros.app.ui.capture

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class SpeechRecognizerHelper(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var silenceJob: Job? = null
    private var lastSpeechTime = System.currentTimeMillis()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Накопленный текст из предыдущих завершенных фраз
    private var accumulatedText = ""

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _transcriptText = MutableStateFlow("")
    val transcriptText: StateFlow<String> = _transcriptText.asStateFlow()

    private val _isSilenceDetected = MutableStateFlow(false)
    val isSilenceDetected: StateFlow<Boolean> = _isSilenceDetected.asStateFlow()

    private val _serviceUnavailable = MutableStateFlow(false)
    val serviceUnavailable: StateFlow<Boolean> = _serviceUnavailable.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Поиск доступного сервиса распознавания речи (особенно важно для китайских прошивок).
     * Прямой поиск пакета com.google.android.tts (Speech recognition & synthesis) или Google App.
     */
    fun findRecognitionService(): ComponentName? {
        val intent = Intent(RecognitionService.SERVICE_INTERFACE)
        val services = try {
            context.packageManager.queryIntentServices(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        if (services.isEmpty()) return null

        // 1. Приоритет Google Speech Services (com.google.android.tts)
        val googleTts = services.find { it.serviceInfo?.packageName == "com.google.android.tts" }
        if (googleTts != null) {
            return ComponentName(googleTts.serviceInfo.packageName, googleTts.serviceInfo.name)
        }

        // 2. Приоритет Google App (com.google.android.googlequicksearchbox)
        val googleSearch = services.find { it.serviceInfo?.packageName == "com.google.android.googlequicksearchbox" }
        if (googleSearch != null) {
            return ComponentName(googleSearch.serviceInfo.packageName, googleSearch.serviceInfo.name)
        }

        // 3. Любой системный сервис распознавания речи
        val first = services.firstOrNull() ?: return null
        return ComponentName(first.serviceInfo.packageName, first.serviceInfo.name)
    }

    fun isServiceAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context) || findRecognitionService() != null
    }

    fun startListening() {
        if (_isListening.value) return

        _serviceUnavailable.value = false
        _permissionDenied.value = false
        _errorMessage.value = null
        accumulatedText = ""
        _transcriptText.value = ""

        if (!isServiceAvailable()) {
            _serviceUnavailable.value = true
            return
        }

        _isListening.value = true
        lastSpeechTime = System.currentTimeMillis()
        startSilenceMonitoring()

        mainHandler.post {
            initAndStartRecognition()
        }
    }

    private fun initAndStartRecognition() {
        if (!_isListening.value) return

        try {
            speechRecognizer?.destroy()
            val serviceComponent = findRecognitionService()
            Log.d("SbrosSpeech", "initAndStartRecognition: serviceComponent=$serviceComponent")

            speechRecognizer = if (serviceComponent != null) {
                SpeechRecognizer.createSpeechRecognizer(context, serviceComponent)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }.apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            Log.d("SbrosSpeech", "startListening intent sent")
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            Log.e("SbrosSpeech", "SecurityException during init", e)
            _permissionDenied.value = true
            stopListening()
        } catch (e: Exception) {
            Log.e("SbrosSpeech", "Exception during init", e)
            _serviceUnavailable.value = true
            stopListening()
        }
    }

    private fun restartListening() {
        if (!_isListening.value) return
        mainHandler.postDelayed({
            if (_isListening.value) {
                initAndStartRecognition()
            }
        }, 150)
    }

    fun stopListening() {
        _isListening.value = false
        _amplitude.value = 0f
        silenceJob?.cancel()

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) { }
            speechRecognizer = null
        }
    }

    fun clearTranscript() {
        accumulatedText = ""
        _transcriptText.value = ""
        lastSpeechTime = System.currentTimeMillis()
        _isSilenceDetected.value = false
    }

    private fun startSilenceMonitoring() {
        silenceJob?.cancel()
        silenceJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive && _isListening.value) {
                delay(400)
                val silentDuration = System.currentTimeMillis() - lastSpeechTime
                if (silentDuration >= 3000L && !_isSilenceDetected.value) {
                    _isSilenceDetected.value = true
                }
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SbrosSpeech", "onReadyForSpeech")
                lastSpeechTime = System.currentTimeMillis()
            }

            override fun onBeginningOfSpeech() {
                Log.d("SbrosSpeech", "onBeginningOfSpeech")
                lastSpeechTime = System.currentTimeMillis()
                _isSilenceDetected.value = false
                _errorMessage.value = null
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Преобразуем реальный уровень dB (-2..10) в нормализованную амплитуду (0..1)
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _amplitude.value = normalized

                if (normalized > 0.2f) {
                    lastSpeechTime = System.currentTimeMillis()
                    _isSilenceDetected.value = false
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("SbrosSpeech", "onEndOfSpeech")
                _amplitude.value = 0f
            }

            override fun onError(error: Int) {
                _amplitude.value = 0f
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                    SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                    SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                    SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                    SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                    else -> "ERROR_CODE_$error"
                }
                Log.e("SbrosSpeech", "onError: $errorMsg ($error)")

                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        _permissionDenied.value = true
                        stopListening()
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER -> {
                        _errorMessage.value = "Ошибка сети речи. Скачай офлайн-пакет в настройках Google или проверь интернет."
                        restartListening()
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Естественная пауза пользователя в речи — продолжаем слушать
                        restartListening()
                    }
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        // Перезапуск сессии распознавания
                        restartListening()
                    }
                    else -> {
                        restartListening()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d("SbrosSpeech", "onResults: $matches")
                if (!matches.isNullOrEmpty()) {
                    val phrase = matches[0].trim()
                    if (phrase.isNotBlank()) {
                        accumulatedText = if (accumulatedText.isBlank()) {
                            phrase
                        } else {
                            "$accumulatedText, $phrase"
                        }
                        _transcriptText.value = accumulatedText
                    }
                }

                lastSpeechTime = System.currentTimeMillis()

                // Бесшовный цикл: перезапуск для следующей фразы, пока открыта шторка
                if (_isListening.value) {
                    restartListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partial = matches[0].trim()
                    if (partial.isNotBlank()) {
                        _transcriptText.value = if (accumulatedText.isBlank()) {
                            partial
                        } else {
                            "$accumulatedText, $partial"
                        }
                        lastSpeechTime = System.currentTimeMillis()
                        _isSilenceDetected.value = false
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
