package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionType
import com.example.data.model.ParsedIntent
import com.example.service.ReminderScheduler
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun IntentBadgeCard(
    intent: ParsedIntent,
    onSpeak: (String) -> Unit,
    onExecuteAction: ((ParsedIntent.DeviceAction) -> Unit)? = null,
    onViewReminders: (() -> Unit)? = null,
    onViewJson: (() -> Unit)? = null,
    executionNote: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("intent_badge_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Intent Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IntentTypeChip(intent = intent)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val speechText = when (intent) {
                                is ParsedIntent.ScheduleReminder -> intent.ttsConfirmation
                                is ParsedIntent.DeviceAction -> intent.ttsConfirmation
                                is ParsedIntent.Unknown -> intent.ttsConfirmation
                            }
                            onSpeak(speechText)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tts_replay_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak confirmation",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onViewJson != null) {
                        IconButton(
                            onClick = onViewJson,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("inspect_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Inspect JSON",
                                tint = ElectricViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body parameters
            when (intent) {
                is ParsedIntent.ScheduleReminder -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = intent.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val parsedEpoch = ReminderScheduler.parseIsoTimestamp(intent.timestampIso)
                        val formatted = if (parsedEpoch != null) {
                            ReminderScheduler.formatTimestamp(parsedEpoch)
                        } else {
                            intent.timestampIso
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        if (executionNote != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Status: $executionNote",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    if (onViewReminders != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onViewReminders,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("view_in_reminders_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View in Reminders Tab")
                        }
                    }
                }

                is ParsedIntent.DeviceAction -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val actionIcon = when (intent.actionType) {
                                ActionType.PHONE_CALL -> Icons.Default.Call
                                ActionType.BROWSE_URL -> Icons.Default.Language
                                ActionType.OPEN_MAPS -> Icons.Default.Map
                                ActionType.OTHER -> Icons.Default.PlayArrow
                            }
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = intent.actionType.name,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = intent.target,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        if (executionNote != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = executionNote,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    if (onExecuteAction != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ElevatedButton(
                            onClick = { onExecuteAction(intent) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("relaunch_device_action_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = NeonCyan,
                                contentColor = Color(0xFF090D16)
                            )
                        ) {
                            val btnLabel = when (intent.actionType) {
                                ActionType.PHONE_CALL -> "Open Phone Dialer"
                                ActionType.BROWSE_URL -> "Open in Browser"
                                ActionType.OPEN_MAPS -> "Open Google Maps"
                                ActionType.OTHER -> "Execute Action"
                            }
                            Text(
                                text = btnLabel,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is ParsedIntent.Unknown -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Informational / Unrecognized Command",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = AccentAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = intent.ttsConfirmation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntentTypeChip(intent: ParsedIntent) {
    val (label, bgColor, textColor, icon) = when (intent) {
        is ParsedIntent.ScheduleReminder -> Quad(
            "schedule_reminder",
            Color(0xFF3B1E6D),
            ElectricViolet,
            Icons.Default.Alarm
        )
        is ParsedIntent.DeviceAction -> Quad(
            "device_action",
            Color(0xFF003844),
            NeonCyan,
            when (intent.actionType) {
                ActionType.PHONE_CALL -> Icons.Default.Call
                ActionType.BROWSE_URL -> Icons.Default.Language
                ActionType.OPEN_MAPS -> Icons.Default.Map
                ActionType.OTHER -> Icons.Default.PlayArrow
            }
        )
        is ParsedIntent.Unknown -> Quad(
            "unknown",
            Color(0xFF452B09),
            AccentAmber,
            Icons.AutoMirrored.Filled.HelpOutline
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
