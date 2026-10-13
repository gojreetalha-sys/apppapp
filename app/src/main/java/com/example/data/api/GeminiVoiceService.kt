package com.example.data.api

import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActionType
import com.example.data.model.ParsedIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class GeminiParseResult(
    val parsedIntent: ParsedIntent,
    val rawJson: String
)

class GeminiVoiceService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiVoiceService"
        private const val BASE_MODEL = "gemini-3.5-flash"
        private const val FALLBACK_MODEL = "gemini-flash-latest"

        const val SYSTEM_INSTRUCTION = """ROLE AND OBJECTIVE
You are the core intelligence engine for a voice assistant app. Your job is to analyze the user's raw speech-to-text input, determine their intent, extract relevant variables, and format the output.

# OUTPUT FORMAT CONSTRAINT
You must ALWAYS respond with a single, valid JSON object. Do not include markdown blocks (like ```json), conversational preambles, or postscripts. If you cannot parse the request, return an error structure.

# CHOSEN INTENTS & SCHEMA EXPECTATIONS

1. Intent: "schedule_reminder"
   - Trigger: User wants to be reminded of something at a specific time.
   - Required JSON Fields:
     {
       "intent": "schedule_reminder",
       "parameters": {
         "title": "String (The action item)",
         "timestamp": "ISO 8601 String (e.g., 2026-10-13T17:00:00Z)",
         "tts_confirmation": "String (Natural speech for the app to speak back)"
       }
     }

2. Intent: "device_action"
   - Trigger: User wants to trigger a native phone capability (Call, Web, Map).
   - Required JSON Fields:
     {
       "intent": "device_action",
       "parameters": {
         "action_type": "String [PHONE_CALL | BROWSE_URL | OPEN_MAPS]",
         "target": "String (The phone number, URL, or address coordinates)",
         "tts_confirmation": "String (Natural speech for the app to speak back)"
       }
     }

3. Intent: "unknown"
   - Trigger: Input is generic conversation, gibberish, or unsupported action.
   - Required JSON Fields:
     {
       "intent": "unknown",
       "parameters": {
         "tts_confirmation": "String (Polite notification that the app cannot perform this action yet)"
       }
     }

# CRITICAL EXECUTION RULES
- Current Base Year Reference: Use the current system date provided by the app layer to resolve relative times like "tomorrow", "next Monday", or "in 2 hours".
- Clean Titles: Remove trigger words from the task title. For example, "Remind me to call Mom" -> Title should just be "Call Mom"."""
    }

    private fun getCurrentIsoTimestamp(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                sdf.timeZone = TimeZone.getDefault()
                sdf.format(Date())
            }
        } catch (_: Exception) {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date())
        }
    }

    suspend fun parseSpeechInput(userInput: String): Result<GeminiParseResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY is missing. Please set it in AI Studio Secrets panel.")
            )
        }

        val currentTimeIso = getCurrentIsoTimestamp()
        val userPromptWithContext = "Current Device System Time: $currentTimeIso\nUser Speech Input: \"$userInput\""

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userPromptWithContext)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", SYSTEM_INSTRUCTION)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        // Try primary model, fallback if needed
        val primaryResult = executeGenerateContent(BASE_MODEL, apiKey, requestJson)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        } else {
            Log.w(TAG, "Primary model $BASE_MODEL failed, trying $FALLBACK_MODEL: ${primaryResult.exceptionOrNull()?.message}")
            return@withContext executeGenerateContent(FALLBACK_MODEL, apiKey, requestJson)
        }
    }

    private fun executeGenerateContent(
        modelName: String,
        apiKey: String,
        requestJson: JSONObject
    ): Result<GeminiParseResult> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(responseBody)
                        errObj.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    return Result.failure(Exception("Gemini API Error ($modelName): $errorMsg"))
                }

                val geminiJson = JSONObject(responseBody)
                val candidates = geminiJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return Result.failure(Exception("Gemini returned no candidates"))
                }

                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

                // Clean potential markdown wrappers
                var cleanedJson = rawText.trim()
                if (cleanedJson.startsWith("```json")) {
                    cleanedJson = cleanedJson.removePrefix("```json").trim()
                } else if (cleanedJson.startsWith("```")) {
                    cleanedJson = cleanedJson.removePrefix("```").trim()
                }
                if (cleanedJson.endsWith("```")) {
                    cleanedJson = cleanedJson.removeSuffix("```").trim()
                }

                val parsedIntent = parseStructuredJson(cleanedJson)
                Result.success(GeminiParseResult(parsedIntent, cleanedJson))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStructuredJson(jsonString: String): ParsedIntent {
        return try {
            val root = JSONObject(jsonString)
            val intent = root.optString("intent", "unknown")
            val params = root.optJSONObject("parameters") ?: JSONObject()
            val ttsConfirmation = params.optString("tts_confirmation", "")

            when (intent) {
                "schedule_reminder" -> {
                    val title = params.optString("title", "Reminder")
                    val timestamp = params.optString("timestamp", "")
                    ParsedIntent.ScheduleReminder(
                        title = title,
                        timestampIso = timestamp,
                        ttsConfirmation = if (ttsConfirmation.isNotBlank()) ttsConfirmation else "Scheduled reminder: $title"
                    )
                }

                "device_action" -> {
                    val actionTypeStr = params.optString("action_type", "").uppercase(Locale.US)
                    val target = params.optString("target", "")
                    val actionType = when (actionTypeStr) {
                        "PHONE_CALL" -> ActionType.PHONE_CALL
                        "BROWSE_URL" -> ActionType.BROWSE_URL
                        "OPEN_MAPS" -> ActionType.OPEN_MAPS
                        else -> ActionType.OTHER
                    }
                    ParsedIntent.DeviceAction(
                        actionType = actionType,
                        target = target,
                        ttsConfirmation = if (ttsConfirmation.isNotBlank()) ttsConfirmation else "Executing $actionTypeStr for $target"
                    )
                }

                "unknown" -> {
                    ParsedIntent.Unknown(
                        ttsConfirmation = if (ttsConfirmation.isNotBlank()) ttsConfirmation else "I'm sorry, I couldn't understand that request."
                    )
                }

                else -> {
                    ParsedIntent.Unknown(
                        ttsConfirmation = if (ttsConfirmation.isNotBlank()) ttsConfirmation else "I couldn't process this request safely."
                    )
                }
            }
        } catch (e: Exception) {
            ParsedIntent.Unknown(
                ttsConfirmation = "I could not parse the response from the intelligence engine: ${e.localizedMessage ?: "Invalid JSON"}"
            )
        }
    }
}
