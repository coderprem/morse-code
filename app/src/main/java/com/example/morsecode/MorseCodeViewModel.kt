package com.example.morsecode

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MorseCodeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MorseCodeUiState())
    val uiState: StateFlow<MorseCodeUiState> = _uiState.asStateFlow()

    private var toneGen: ToneGenerator? = null
    private val morseCodeMap = createMorseCodeMap()
    private var conversionJob: Job? = null

    fun updateText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun convertToMorse() {
        // Cancel any ongoing conversion
        conversionJob?.cancel()

        _uiState.update { state ->
            val morseCode = state.inputText.uppercase()
                .map { morseCodeMap[it] ?: "" }
                .joinToString(" ")

            state.copy(
                morseCode = morseCode,
                displayedText = ""
            )
        }

        conversionJob = viewModelScope.launch {
            _uiState.value.morseCode.forEachIndexed { index, _ ->
                try {
                    delay(80L) // Typing speed
                    _uiState.update { it.copy(displayedText = it.morseCode.take(index + 1)) }
                } catch (_: CancellationException) {
                    // Conversion was cancelled, exit gracefully
                    return@launch
                }
            }
        }
    }
    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        if (_uiState.value.isPlaying || _uiState.value.morseCode.isEmpty()) return

        _uiState.update { it.copy(isPlaying = true) }

        viewModelScope.launch {
            try {
                toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                playMorseCode(_uiState.value.morseCode)
            } catch (e: Exception) {
                Log.e("MorseCode", "Error playing Morse code", e)
            } finally {
                stopPlayback()
            }
        }
    }

    private suspend fun playMorseCode(code: String) {
        // Morse code timing constants (in ms)
        val dotDuration = 120
        val dashDuration = 360
        val elementGap = 100
        val shortGap = 300
        val mediumGap = 700

        // Different frequencies for dot and dash
        val dotFrequency = 1200
        val dashFrequency = 600

        for (char in code) {
            if (!_uiState.value.isPlaying) break

            when (char) {
                '.' -> {
                    toneGen?.startTone(ToneGenerator.TONE_DTMF_S, dotFrequency)
                    delay(dotDuration.toLong())
                    toneGen?.stopTone()
                    delay(elementGap.toLong())
                }
                '-' -> {
                    toneGen?.startTone(ToneGenerator.TONE_DTMF_D, dashFrequency)
                    delay(dashDuration.toLong())
                    toneGen?.stopTone()
                    delay(elementGap.toLong())
                }
                ' ' -> delay(shortGap.toLong())
                '/' -> delay(mediumGap.toLong())
            }
        }
    }

    private fun stopPlayback() {
        _uiState.update { it.copy(isPlaying = false) }
        toneGen?.release()
        toneGen = null
    }

    override fun onCleared() {
        super.onCleared()
        conversionJob?.cancel()
        stopPlayback()
    }

    private fun createMorseCodeMap(): Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", ' ' to "/",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
        ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
    )
}
