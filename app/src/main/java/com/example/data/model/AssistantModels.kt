package com.example.data.model

sealed class ParsedIntent {
    data class ScheduleReminder(
        val title: String,
        val timestampIso: String,
        val ttsConfirmation: String
    ) : ParsedIntent()

    data class DeviceAction(
        val actionType: ActionType,
        val target: String,
        val ttsConfirmation: String
    ) : ParsedIntent()

    data class Unknown(
        val ttsConfirmation: String
    ) : ParsedIntent()
}

enum class ActionType {
    PHONE_CALL,
    BROWSE_URL,
    OPEN_MAPS,
    OTHER
}

data class MessageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val parsedIntent: ParsedIntent? = null,
    val rawJson: String? = null,
    val isError: Boolean = false,
    val actionExecuted: Boolean = false,
    val executionNote: String? = null
)
