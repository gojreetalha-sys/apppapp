package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageItem
import com.example.data.model.ParsedIntent
import com.example.ui.components.IntentBadgeCard
import com.example.ui.components.QuickPromptsRow
import com.example.ui.components.VoiceWaveVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToReminders: () -> Unit,
    onNavigateToJsonInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val partialSpeech by viewModel.partialSpeech.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isProcessing) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Active visualizer banner if listening or speaking
        AnimatedVisibility(
            visible = isListening || isSpeaking,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val statusText = if (isListening) {
                        if (partialSpeech.isNotBlank()) partialSpeech else "Listening to your voice..."
                    } else {
                        "Assistant Speaking..."
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (isSpeaking) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Speaking",
                                tint = ElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                VoiceWaveVisualizer(isActive = true)
            }
        }

        // Status Error/Info Banner
        if (statusMessage != null) {
            Surface(
                color = DarkSurfaceHighlight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearStatusMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    item = msg,
                    onSpeak = { text -> viewModel.speakText(text) },
                    onExecuteAction = { action -> viewModel.reExecuteDeviceAction(action) },
                    onViewReminders = onNavigateToReminders,
                    onViewJson = onNavigateToJsonInspector
                )
            }

            if (isProcessing) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Analyzing intent & structured variables...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Quick prompt suggestions
        QuickPromptsRow(
            onSelectPrompt = { prompt ->
                textInput = prompt
                viewModel.processUserCommand(prompt)
                textInput = ""
            }
        )

        // Bottom Input Row
        InputBar(
            textInput = textInput,
            onTextChanged = { textInput = it },
            onSend = {
                if (textInput.isNotBlank()) {
                    viewModel.processUserCommand(textInput)
                    textInput = ""
                }
            },
            isListening = isListening,
            onToggleListen = {
                if (isListening) {
                    viewModel.stopListening()
                } else {
                    viewModel.startListening()
                }
            },
            isProcessing = isProcessing
        )
    }
}

@Composable
fun MessageBubble(
    item: MessageItem,
    onSpeak: (String) -> Unit,
    onExecuteAction: ((ParsedIntent.DeviceAction) -> Unit)?,
    onViewReminders: () -> Unit,
    onViewJson: () -> Unit
) {
    if (item.isUser) {
        // User message bubble aligned to the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF005B66), Color(0xFF0284C7))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    } else {
        // Assistant Message aligned to the left
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NeonCyan, ElectricViolet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF090D16),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Voice Engine",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Text bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (item.isError) Color(0xFFFCA5A5) else TextPrimary
                        )
                    )
                }

                // If intent was structured, display Intent Badge Card!
                if (item.parsedIntent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    IntentBadgeCard(
                        intent = item.parsedIntent,
                        onSpeak = onSpeak,
                        onExecuteAction = onExecuteAction,
                        onViewReminders = onViewReminders,
                        onViewJson = onViewJson,
                        executionNote = item.executionNote
                    )
                }
            }
        }
    }
}

@Composable
fun InputBar(
    textInput: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    onToggleListen: () -> Unit,
    isProcessing: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_mic")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse Mic Button
            FloatingActionButton(
                onClick = onToggleListen,
                modifier = Modifier
                    .size(52.dp)
                    .scale(if (isListening) pulseScale else 1f)
                    .testTag("mic_toggle_button"),
                containerColor = if (isListening) ElectricViolet else NeonCyan,
                contentColor = if (isListening) Color.White else Color(0xFF090D16),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start voice recognition",
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text input
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                placeholder = {
                    Text(
                        text = if (isListening) "Listening..." else "Type or tap mic...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                maxLines = 3,
                singleLine = false,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSend() }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceHighlight,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send icon button
            IconButton(
                onClick = onSend,
                enabled = textInput.isNotBlank() && !isProcessing,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command",
                    tint = if (textInput.isNotBlank() && !isProcessing) NeonCyan else TextSecondary
                )
            }
        }
    }
}
