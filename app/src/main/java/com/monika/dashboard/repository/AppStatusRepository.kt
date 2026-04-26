package me.elvish.statusreporter.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.elvish.statusreporter.data.SettingsStore

import kotlinx.coroutines.flow.combine
import me.elvish.statusreporter.service.AppForegroundService

object AppStatusRepository {
    var currentForegroundApp: String = "android"
    
    var emoji: String = "📱"
        private set

    fun init(context: Context, scope: CoroutineScope) {
        val settings = SettingsStore(context.applicationContext)

        settings.customEmojiMap.onEach { emojiStr ->
            emoji = emojiStr.takeIf { it.isNotBlank() } ?: "📱"
        }.launchIn(scope)

        combine(
            settings.monitoringEnabled,
            settings.foregroundServiceEnabled
        ) { monitoring, foreground ->
            monitoring to foreground
        }.onEach { (monitoring, foreground) ->
            if (monitoring && foreground) {
                AppForegroundService.start(context)
            } else {
                AppForegroundService.stop(context)
            }
        }.launchIn(scope)
    }
}
