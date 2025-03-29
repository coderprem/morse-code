package com.example.morsecode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.morsecode.ui.theme.MorseCodeTheme


/**
 * Morse Code Translator App
 *
 * Features:
 * - Real-time text to Morse code conversion
 * - Audio playback of Morse code with distinct dot/dash sounds
 * - Animated visual display with cursor
 * - Proper lifecycle management for audio resources
 * - Clean architecture with ViewModel
 */

// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MorseCodeTheme {
                MorseCodeApp()
            }
        }
    }
}

/**
 * Main application composable that sets up the scaffold and theming
 */
@Composable
fun MorseCodeApp() {
    MorseCodeTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            MorseCodeTranslator(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Main screen composable containing all UI components
 */
@Composable
fun MorseCodeTranslator(modifier: Modifier = Modifier) {
    val viewModel: MorseCodeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    MorseCodeContent(
        uiState = uiState,
        onTextChange = viewModel::updateText,
        onConvert = viewModel::convertToMorse,
        onPlayStop = viewModel::togglePlayback,
        modifier = modifier
    )
}

/**
 * Stateless UI component that displays Morse code translator
 */
@Composable
private fun MorseCodeContent(
    uiState: MorseCodeUiState,
    onTextChange: (String) -> Unit,
    onConvert: () -> Unit,
    onPlayStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Cursor blink animation
    val infiniteTransition = rememberInfiniteTransition()
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Auto-scroll when text updates
    LaunchedEffect(uiState.displayedText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Morse Code Translator",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Morse code output display
        MorseCodeDisplay(
            text = uiState.displayedText,
            fullText = uiState.morseCode,
            cursorAlpha = cursorAlpha,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 300.dp)
        )

        // Input and controls
        MorseCodeControls(
            inputText = uiState.inputText,
            isPlaying = uiState.isPlaying,
            hasMorseCode = uiState.morseCode.isNotEmpty(),
            onTextChange = onTextChange,
            onConvert = {
                onConvert()
                keyboardController?.hide()
            },
            onPlayStop = onPlayStop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


/**
 * Component that displays the Morse code output with animations
 */
@Composable
private fun MorseCodeDisplay(
    text: String,
    fullText: String,
    cursorAlpha: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 12.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (fullText.isEmpty()) {
                Text(
                    text = "Morse code will appear here",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        append(text)
                        if (text.length < fullText.length) {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary.copy(
                                        alpha = cursorAlpha
                                    )
                                )
                            ) {
                                append("│") // Blinking cursor
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * Component containing input field and control buttons
 */
@Composable
private fun MorseCodeControls(
    inputText: String,
    isPlaying: Boolean,
    hasMorseCode: Boolean,
    onTextChange: (String) -> Unit,
    onConvert: () -> Unit,
    onPlayStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Input field
        OutlinedTextField(
            value = inputText,
            onValueChange = onTextChange,
            label = { Text("Enter text") },
            trailingIcon = {
                IconButton(
                    onClick = onConvert,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Convert to Morse",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConvert() }),
            modifier = Modifier.weight(1f)
        )

        // Play/Stop button
        IconButton(
            onClick = onPlayStop,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isPlaying) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            enabled = hasMorseCode
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Stop playback" else "Play Morse code",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ViewModel and Data Classes
data class MorseCodeUiState(
    val inputText: String = "",
    val morseCode: String = "",
    val displayedText: String = "",
    val isPlaying: Boolean = false
)

