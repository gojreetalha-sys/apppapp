package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.ActionType
import com.example.data.model.ParsedIntent

sealed class ActionResult {
    data class Success(val message: String, val intentLaunched: Boolean) : ActionResult()
    data class Failure(val error: String) : ActionResult()
}

class DeviceActionExecutor(private val context: Context) {

    fun executeDeviceAction(action: ParsedIntent.DeviceAction): ActionResult {
        return try {
            when (action.actionType) {
                ActionType.PHONE_CALL -> {
                    val cleanPhone = action.target.trim()
                    val dialUri = Uri.parse("tel:$cleanPhone")
                    val dialIntent = Intent(Intent.ACTION_DIAL, dialUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                    ActionResult.Success("Opened phone dialer for $cleanPhone", true)
                }

                ActionType.BROWSE_URL -> {
                    var targetUrl = action.target.trim()
                    if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                        targetUrl = "https://$targetUrl"
                    }
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                    ActionResult.Success("Opened web link: $targetUrl", true)
                }

                ActionType.OPEN_MAPS -> {
                    val query = action.target.trim()
                    val mapsUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(mapIntent)
                    } catch (_: Exception) {
                        // Web fallback for maps
                        val webMapsIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(webMapsIntent)
                    }
                    ActionResult.Success("Opened maps search for '$query'", true)
                }

                ActionType.OTHER -> {
                    ActionResult.Failure("Unsupported device action type")
                }
            }
        } catch (e: Exception) {
            ActionResult.Failure("Failed to launch action: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
