package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary

data class QuickPrompt(
    val title: String,
    val icon: ImageVector,
    val text: String
)

val samplePrompts = listOf(
    QuickPrompt("Remind 5 PM", Icons.Default.Alarm, "Remind me to call Mom at 5 PM"),
    QuickPrompt("Water in 30m", Icons.Default.Alarm, "Remind me to drink water in 30 minutes"),
    QuickPrompt("Call 555-0199", Icons.Default.Call, "Call 555-0199"),
    QuickPrompt("Open GitHub", Icons.Default.Language, "Open github.com"),
    QuickPrompt("Search Cafe", Icons.Default.Map, "Search coffee shop on Google Maps"),
    QuickPrompt("Ask Question", Icons.Default.QuestionMark, "What is the distance to Mars?")
)

@Composable
fun QuickPromptsRow(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("quick_prompts_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        samplePrompts.forEach { prompt ->
            val iconTint = when (prompt.icon) {
                Icons.Default.Alarm -> ElectricViolet
                Icons.Default.Call, Icons.Default.Language, Icons.Default.Map -> NeonCyan
                else -> AccentAmber
            }

            AssistChip(
                onClick = { onSelectPrompt(prompt.text) },
                label = {
                    Text(
                        text = prompt.title,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = prompt.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = DarkSurfaceVariant
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = DarkSurfaceVariant
                )
            )
        }
    }
}
