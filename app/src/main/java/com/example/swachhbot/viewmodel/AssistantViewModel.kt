package com.example.swachhbot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.swachhbot.db.SwachhDatabase
import com.example.swachhbot.network.AssistantChatRequest
import com.example.swachhbot.network.BackendClient
import com.example.swachhbot.network.BackendConfig
import com.example.swachhbot.network.HouseIdentity
import com.example.swachhbot.repository.HouseRepository
import com.example.swachhbot.repository.impl.RoomHouseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** An action the assistant prepared and that needs the user's confirmation. */
data class AssistantActionUi(
    val id: String,
    val summary: String,
    val actionType: String,
    val status: String
) {
    val isPending: Boolean get() = status == "PENDING"
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val reasoning: List<String> = emptyList(),
    val actions: List<AssistantActionUi> = emptyList(),
    val isError: Boolean = false
)

/**
 * Drives the in-app assistant chat.
 *
 * Conversation context is kept on the server; the ViewModel only remembers the
 * conversation id. If the backend is unreachable the assistant degrades to
 * answering from the local Room database.
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val client = BackendClient(BackendConfig.BASE_URL)
    private val houseId = HouseIdentity.get(application)
    private val robotId = "swachhbot-01"
    private val repository: HouseRepository =
        RoomHouseRepository(SwachhDatabase.getDatabase(application))

    private val _messages = MutableStateFlow(listOf(greetingMessage()))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private var conversationId: String? = null

    // ------------------------------------------------------------------ send

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _busy.value) return

        _messages.value = _messages.value + ChatMessage(fromUser = true, text = trimmed)
        _busy.value = true

        viewModelScope.launch {
            val response = runCatching {
                client.api.assistantChat(
                    AssistantChatRequest(
                        houseId = houseId,
                        robotId = robotId,
                        conversationId = conversationId,
                        message = trimmed
                    )
                )
            }.getOrNull()

            _messages.value = _messages.value + if (response != null) {
                conversationId = response.conversationId
                ChatMessage(
                    fromUser = false,
                    text = response.reply,
                    reasoning = response.reasoning.map { it.note }.distinct(),
                    actions = response.pendingActions.map {
                        AssistantActionUi(it.id, it.summary, it.actionType, it.status)
                    }
                )
            } else {
                ChatMessage(fromUser = false, text = localAnswer(trimmed), isError = true)
            }
            _busy.value = false
        }
    }

    // -------------------------------------------------------- confirm / reject

    fun confirm(actionId: String) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                client.api.confirmAssistantAction(actionId, robotId)
            }.getOrNull()

            val message = when {
                result == null -> "Could not reach the robot service to confirm that action."
                result.status == "EXECUTED" ->
                    "Done — ${result.summary.lowercase()} has been sent to the robot."
                else -> "Action is now ${result.status}."
            }
            updateAction(actionId, result?.status ?: "FAILED")
            _messages.value = _messages.value + ChatMessage(fromUser = false, text = message)
            _busy.value = false
        }
    }

    fun reject(actionId: String) {
        viewModelScope.launch {
            runCatching { client.api.rejectAssistantAction(actionId) }
            updateAction(actionId, "REJECTED")
            _messages.value = _messages.value +
                ChatMessage(fromUser = false, text = "Cancelled — nothing was sent to the robot.")
        }
    }

    fun clear() {
        conversationId = null
        _messages.value = listOf(greetingMessage())
    }

    // ------------------------------------------------------------- internals

    private fun updateAction(actionId: String, status: String) {
        _messages.value = _messages.value.map { msg ->
            msg.copy(actions = msg.actions.map {
                if (it.id == actionId) it.copy(status = status) else it
            })
        }
    }

    /**
     * Very small offline fallback so the chat is still useful without the
     * backend: answers from the local Room memory only.
     */
    private suspend fun localAnswer(question: String): String = withContext(Dispatchers.IO) {
        val objects = runCatching {
            repository.getObjects(HouseIdentity.LOCAL_HOUSE_ID).first()
        }.getOrDefault(emptyList())

        val prefix = "I can't reach my reasoning service right now, so here's what I know locally.\n\n"
        when {
            question.contains("learn", true) ->
                prefix + if (objects.isEmpty()) {
                    "I haven't detected any objects yet."
                } else {
                    "I remember ${objects.size} object(s): " +
                        objects.sortedByDescending { it.detectionCount }.take(6)
                            .joinToString(", ") { "${it.type} (${it.detectionCount}x)" } + "."
                }

            question.contains("dirty", true) || question.contains("often", true) ->
                prefix + "Ranking rooms by dirt needs the learning service, which needs the backend."

            else ->
                prefix + "Start the SwachhBot backend and I'll be able to answer questions about " +
                    "your house, plan cleaning runs and report where I got stuck."
        }
    }

    private companion object {
        fun greetingMessage() = ChatMessage(
            fromUser = false,
            text = "Hi! I'm SwachhBot. Ask me to clean a room, or ask what I've learned about " +
                "your house. Anything that moves me will ask you to confirm first."
        )
    }
}
