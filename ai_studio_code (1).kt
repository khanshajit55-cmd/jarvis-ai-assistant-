package com.jarvis.assistant.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jarvis.assistant.model.ActionExecutionResult
import com.jarvis.assistant.model.ActionRequest
import com.jarvis.assistant.model.ActionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafeActionRouter(private val context: Context) {

    // Common app packagename dictionary for popular apps on Infinix Hot 40i
    private val appPackageMap = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "camera" to "camera_intent",
        "gallery" to "gallery_intent",
        "settings" to "com.android.settings",
        "calculator" to "calculator_intent",
        "facebook" to "com.facebook.katana",
        "messenger" to "com.facebook.orca",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "clock" to "com.google.android.deskclock",
        "telegram" to "org.telegram.messenger",
        "imo" to "com.imo.android.imoim",
        "tiktok" to "com.zhiliaoapp.musically"
    )

    fun parseCommand(rawText: String): ActionRequest? {
        val text = rawText.lowercase().trim()

        // 1. Phone Call Intent (Bangla, Banglish, English)
        val phoneMatch = Regex("(?:call|ফোন করো|কল দাও|dial)\\s+([+0-9\\s-]{6,})").find(text)
        if (phoneMatch != null) {
            val number = phoneMatch.groupValues[1].replace("\\s|-".toRegex(), "")
            return ActionRequest(
                type = ActionType.START_PHONE_CALL,
                title = "Initiate Phone Call",
                description = "Call number: $number. High consequence action requiring explicit confirmation.",
                requiresConfirmation = true,
                target = number
            )
        }

        // 2. Alarm Intent (Bangla: এলার্ম দাও, সকাল ৭টায় ডাকো / English: set alarm for 7:30)
        val alarmMatch = Regex("(?:set alarm|এলার্ম দাও|alarm for|সকাল|বিকাল|রাত)\\s*([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm|টা)?").find(text)
        if (alarmMatch != null || text.contains("alarm") || text.contains("এলার্ম")) {
            val hour = alarmMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
            val minute = alarmMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
            val ampm = alarmMatch?.groupValues?.getOrNull(3) ?: ""
            val isPm = ampm.contains("pm") || text.contains("বিকাল") || text.contains("রাত")
            val finalHour = if (isPm && hour < 12) hour + 12 else hour

            return ActionRequest(
                type = ActionType.SET_ALARM,
                title = "Set Device Alarm",
                description = "Set alarm for %02d:%02d".format(finalHour, minute),
                requiresConfirmation = false,
                target = "%02d:%02d".format(finalHour, minute),
                extraData = mapOf("hour" to finalHour.toString(), "minute" to minute.toString())
            )
        }

        // 3. Time & Date (কী বার, কয়টা বাজে, what time is it)
        if (text.contains("time") || text.contains("date") || text.contains("কয়টা বাজে") || text.contains("সময়") || text.contains("কী বার")) {
            return ActionRequest(
                type = ActionType.GET_TIME_DATE,
                title = "Query Time & Date",
                description = "Read current system clock and date.",
                requiresConfirmation = false
            )
        }

        // 4. Open Settings
        if (text.contains("open settings") || text.contains("সেটিংস") || text.contains("wifi settings") || text.contains("bluetooth")) {
            val subSetting = when {
                text.contains("wifi") || text.contains("ওয়াইফাই") -> "wifi"
                text.contains("bluetooth") || text.contains("ব্লুটুথ") -> "bluetooth"
                else -> "general"
            }
            return ActionRequest(
                type = ActionType.OPEN_SETTINGS,
                title = "Open Android Settings",
                description = "Launch system $subSetting settings.",
                requiresConfirmation = false,
                target = subSetting
            )
        }

        // 5. Open Apps (খোল, open, launch)
        if (text.startsWith("open ") || text.startsWith("launch ") || text.contains("খোল") || text.contains("ওপেন কর")) {
            for ((key, _) in appPackageMap) {
                if (text.contains(key)) {
                    return ActionRequest(
                        type = ActionType.OPEN_APP,
                        title = "Open App: ${key.replaceFirstChar { it.uppercase() }}",
                        description = "Launch installed application package via Intent.",
                        requiresConfirmation = false,
                        target = key
                    )
                }
            }
        }

        // 6. Web Search
        if (text.startsWith("search ") || text.startsWith("google ") || text.contains("খুঁজে বের কর")) {
            val query = text.replace("search", "").replace("google", "").replace("খুঁজে বের কর", "").trim()
            if (query.isNotBlank()) {
                return ActionRequest(
                    type = ActionType.SEARCH_WEB,
                    title = "Web Search",
                    description = "Search web for: $query",
                    requiresConfirmation = false,
                    target = query
                )
            }
        }

        // 7. Clear Memory (Consequential - requires confirmation)
        if (text.contains("clear memory") || text.contains("delete memory") || text.contains("সব ভুলে যাও") || text.contains("মেমরি মুছ")) {
            return ActionRequest(
                type = ActionType.CLEAR_MEMORY,
                title = "Wipe Local Memory",
                description = "Permanently purge all saved preferences and user memory facts.",
                requiresConfirmation = true,
                target = "all_memory"
            )
        }

        return null
    }

    fun executeAction(request: ActionRequest, userConfirmed: Boolean = false): ActionExecutionResult {
        if (request.requiresConfirmation && !userConfirmed) {
            return ActionExecutionResult(
                success = false,
                message = "Action '${request.title}' requires explicit user confirmation before execution."
            )
        }

        return when (request.type) {
            ActionType.GET_TIME_DATE -> {
                val now = Date()
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
                val dateStr = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(now)
                ActionExecutionResult(
                    success = true,
                    message = "Current time is $timeStr ($dateStr)."
                )
            }

            ActionType.OPEN_APP -> {
                val target = request.target.lowercase()
                when (target) {
                    "camera" -> {
                        val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        return tryLaunchIntent(cameraIntent, "Camera launched successfully.")
                    }
                    "calculator" -> {
                        val calcIntent = Intent().apply {
                            action = Intent.ACTION_MAIN
                            addCategory(Intent.CATEGORY_APP_CALCULATOR)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        return tryLaunchIntent(calcIntent, "Calculator launched.")
                    }
                    else -> {
                        val pkg = appPackageMap[target] ?: target
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            ActionExecutionResult(
                                success = true,
                                message = "Launched app $target ($pkg).",
                                intentActionDispatched = Intent.ACTION_MAIN
                            )
                        } else {
                            ActionExecutionResult(
                                success = false,
                                message = "App '$target' ($pkg) is not installed on this Infinix device."
                            )
                        }
                    }
                }
            }

            ActionType.OPEN_SETTINGS -> {
                val intentAction = when (request.target) {
                    "wifi" -> Settings.ACTION_WIFI_SETTINGS
                    "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                    else -> Settings.ACTION_SETTINGS
                }
                val intent = Intent(intentAction).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                tryLaunchIntent(intent, "Opened ${request.target} settings.")
            }

            ActionType.SET_ALARM -> {
                val hour = request.extraData["hour"]?.toIntOrNull() ?: 7
                val minute = request.extraData["minute"]?.toIntOrNull() ?: 0
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                tryLaunchIntent(intent, "Alarm configured for %02d:%02d.".format(hour, minute))
            }

            ActionType.START_PHONE_CALL -> {
                val number = request.target
                if (number.isBlank()) {
                    return ActionExecutionResult(success = false, message = "No valid phone number provided.")
                }

                val hasCallPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED

                val intent = if (hasCallPermission) {
                    Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                tryLaunchIntent(intent, "Calling $number verified by user.")
            }

            ActionType.SEARCH_WEB -> {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, request.target)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                tryLaunchIntent(intent, "Web search dispatched for: ${request.target}")
            }

            ActionType.CLEAR_MEMORY -> {
                ActionExecutionResult(
                    success = true,
                    message = "Memory purge request authorized and dispatched."
                )
            }

            else -> {
                ActionExecutionResult(
                    success = false,
                    message = "Command not permitted. JARVIS strictly rejects arbitrary shell execution or unsafe system modifications."
                )
            }
        }
    }

    private fun tryLaunchIntent(intent: Intent, successMsg: String): ActionExecutionResult {
        return try {
            context.startActivity(intent)
            ActionExecutionResult(
                success = true,
                message = successMsg,
                intentActionDispatched = intent.action
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                success = false,
                message = "Failed to launch intent: ${e.localizedMessage ?: "Activity not found"}"
            )
        }
    }
}