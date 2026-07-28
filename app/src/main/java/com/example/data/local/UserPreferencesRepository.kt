package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.BotInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cloudhub_user_prefs", Context.MODE_PRIVATE)

    private val _botInfo = MutableStateFlow(loadBotInfo())
    val botInfo: StateFlow<BotInfo> = _botInfo

    private fun loadBotInfo(): BotInfo {
        val token = prefs.getString("bot_token", "") ?: ""
        val chatId = prefs.getString("chat_id", "") ?: ""
        val botName = prefs.getString("bot_name", "") ?: ""
        val username = prefs.getString("bot_username", "") ?: ""
        val isConnected = prefs.getBoolean("is_connected", false)
        val isDemoMode = prefs.getBoolean("is_demo_mode", false)

        return BotInfo(
            token = token,
            chatId = chatId,
            botName = botName,
            username = username,
            isConnected = isConnected,
            isDemoMode = isDemoMode
        )
    }

    fun saveBotCredentials(
        token: String,
        chatId: String,
        botName: String,
        username: String,
        isDemoMode: Boolean = false
    ) {
        prefs.edit()
            .putString("bot_token", token)
            .putString("chat_id", chatId)
            .putString("bot_name", botName)
            .putString("bot_username", username)
            .putBoolean("is_connected", true)
            .putBoolean("is_demo_mode", isDemoMode)
            .apply()

        _botInfo.value = BotInfo(
            token = token,
            chatId = chatId,
            botName = botName,
            username = username,
            isConnected = true,
            isDemoMode = isDemoMode
        )
    }

    fun enableDemoMode() {
        saveBotCredentials(
            token = "demo_bot_token",
            chatId = "demo_chat_123",
            botName = "CloudHub Demo Bot",
            username = "cloudhub_demo_bot",
            isDemoMode = true
        )
    }

    fun clearCredentials() {
        prefs.edit()
            .remove("bot_token")
            .remove("chat_id")
            .remove("bot_name")
            .remove("bot_username")
            .putBoolean("is_connected", false)
            .putBoolean("is_demo_mode", false)
            .apply()

        _botInfo.value = BotInfo(
            token = "",
            chatId = "",
            botName = "",
            username = "",
            isConnected = false,
            isDemoMode = false
        )
    }
}
