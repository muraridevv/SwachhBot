package com.example.swachhbot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Top-level screens of the app. */
enum class AppScreen { SIMULATOR, LEARNING, ASSISTANT }

/**
 * Minimal in-process navigation:
 * the simulator, the "🧠 What the Robot Learned" dashboard, and the AI assistant.
 */
@Composable
fun SwachhBotApp() {
    var screen by remember { mutableStateOf(AppScreen.SIMULATOR) }

    when (screen) {
        AppScreen.SIMULATOR -> SimulatorScreen(
            onOpenLearning = { screen = AppScreen.LEARNING },
            onOpenAssistant = { screen = AppScreen.ASSISTANT }
        )

        AppScreen.LEARNING -> LearningScreen(
            onBack = { screen = AppScreen.SIMULATOR }
        )

        AppScreen.ASSISTANT -> AssistantScreen(
            onBack = { screen = AppScreen.SIMULATOR }
        )
    }
}
