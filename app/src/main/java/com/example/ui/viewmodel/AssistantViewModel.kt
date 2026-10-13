package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GeminiVoiceService
import com.example.data.db.AppDatabase
import com.example.data.db.ReminderEntity
import com.example.data.model.ActionType
import com.example.data.model.MessageItem
import com.example.data.model.ParsedIntent
import com.example.service.DeviceActionExecutor
import com.example.service.ReminderScheduler
import com.example.service.SpeechRecognitionManager
import com.example.service.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val reminderDao = db.reminderDao()
    private val reminderScheduler = ReminderScheduler(application)
    private val deviceActionExecutor = DeviceActionExecutor(application)
    private val geminiService = GeminiVoiceService()

    val ttsManager = TextToSpeechManager(application)

    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _latestRawJson = MutableStateFlow<String?>(null)
    val latestRawJson: StateFlow<String?> = _latestRawJson.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val isSpeaking = ttsManager.isSpeaking
    val isMuted = ttsManager.isMuted

    val reminders: StateFlow<List<ReminderEntity>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<ReminderEntity>> = reminderDao.getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechManager = SpeechRecognitionManager(
        context = application,
        onResultReceived = { recognizedText ->
            processUserCommand(recognizedText)
        },
        onErrorReceived = { error ->
            _statusMessage.value = error
        }
    )

    val isListening = speechManager.isListening
    val partialSpeech = speechManager.partialText

    init {
        // Welcome message with sample actions
        val welcomeMsg = MessageItem(
            text = "Hello! I am your AI Voice Assistant. Speak or type to schedule reminders, dial numbers, open links, or search maps.",
            isUser = false,
            parsedIntent = ParsedIntent.Unknown("Ready to help you.")
        )
        _messages.value = listOf(welcomeMsg)
    }

    fun processUserCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || _isProcessing.value) return

        // Add user message
        val userMsg = MessageItem(text = trimmed, isUser = true)
        _messages.value = _messages.value + userMsg
        _isProcessing.value = true
        _statusMessage.value = null

        viewModelScope.launch {
            val result = geminiService.parseSpeechInput(trimmed)
            _isProcessing.value = false

            result.onSuccess { parseResult ->
                val parsed = parseResult.parsedIntent
                _latestRawJson.value = parseResult.rawJson

                var note: String? = null
                var executed = false

                // 1. Always speak back the confirmation first
                val ttsText = when (parsed) {
                    is ParsedIntent.ScheduleReminder -> parsed.ttsConfirmation
                    is ParsedIntent.DeviceAction -> parsed.ttsConfirmation
                    is ParsedIntent.Unknown -> parsed.ttsConfirmation
                }
                ttsManager.speak(ttsText)

                // 2. Route the action to the correct system service
                when (parsed) {
                    is ParsedIntent.ScheduleReminder -> {
                        val parsedTimeMillis = ReminderScheduler.parseIsoTimestamp(parsed.timestampIso)
                            ?: (System.currentTimeMillis() + 60 * 1000) // fallback 1 min if unparseable

                        val reminder = ReminderEntity(
                            title = parsed.title,
                            timestampIso = parsed.timestampIso,
                            triggerTimeMillis = parsedTimeMillis,
                            ttsConfirmation = parsed.ttsConfirmation
                        )
                        val insertedId = reminderDao.insertReminder(reminder)
                        reminderScheduler.scheduleReminder(
                            reminderId = insertedId,
                            title = parsed.title,
                            triggerTimeMillis = parsedTimeMillis,
                            ttsConfirmation = parsed.ttsConfirmation
                        )
                        executed = true
                        note = "Scheduled for: ${ReminderScheduler.formatTimestamp(parsedTimeMillis)}"
                    }

                    is ParsedIntent.DeviceAction -> {
                        val actionRes = deviceActionExecutor.executeDeviceAction(parsed)
                        executed = when (actionRes) {
                            is com.example.service.ActionResult.Success -> {
                                note = actionRes.message
                                true
                            }
                            is com.example.service.ActionResult.Failure -> {
                                note = actionRes.error
                                false
                            }
                        }
                    }

                    is ParsedIntent.Unknown -> {
                        note = "No action scheduled."
                        executed = false
                    }
                }

                val assistantMsg = MessageItem(
                    text = ttsText,
                    isUser = false,
                    parsedIntent = parsed,
                    rawJson = parseResult.rawJson,
                    actionExecuted = executed,
                    executionNote = note
                )
                _messages.value = _messages.value + assistantMsg
            }.onFailure { error ->
                val errorMsg = error.localizedMessage ?: "Failed to reach AI service"
                _statusMessage.value = errorMsg
                val assistantMsg = MessageItem(
                    text = "Sorry, I encountered an issue: $errorMsg",
                    isUser = false,
                    isError = true
                )
                _messages.value = _messages.value + assistantMsg
                ttsManager.speak("Sorry, I could not complete that request.")
            }
        }
    }

    fun startListening() {
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun toggleMute() {
        ttsManager.toggleMute()
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    fun cancelReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(reminder.id)
            reminderDao.markCancelled(reminder.id)
            _statusMessage.value = "Cancelled reminder: ${reminder.title}"
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(reminder.id)
            reminderDao.deleteReminder(reminder)
            _statusMessage.value = "Deleted reminder: ${reminder.title}"
        }
    }

    fun reExecuteDeviceAction(action: ParsedIntent.DeviceAction) {
        val result = deviceActionExecutor.executeDeviceAction(action)
        when (result) {
            is com.example.service.ActionResult.Success -> {
                _statusMessage.value = result.message
            }
            is com.example.service.ActionResult.Failure -> {
                _statusMessage.value = result.error
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
        ttsManager.release()
    }
}
