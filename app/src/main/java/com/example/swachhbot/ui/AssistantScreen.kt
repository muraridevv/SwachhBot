package com.example.swachhbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.viewmodel.AssistantActionUi
import com.example.swachhbot.viewmodel.AssistantViewModel
import com.example.swachhbot.viewmodel.ChatMessage

private val BACKGROUND = Color(0xFF142420)
private val MINT = Color(0xFFB8FFCF)
private val USER_BUBBLE = Color(0xFF2E5B4C)
private val BOT_BUBBLE = Color(0xFF1E332C)
private val ACTION_CARD = Color(0xFF1B3A32)
private val YELLOW = Color(0xFFFFD166)

private val SUGGESTIONS = listOf(
    "Clean the kitchen.",
    "Don't clean the bedroom.",
    "Where did you get stuck yesterday?",
    "What did you learn about my house?",
    "Which room gets dirty most often?",
    "Clean the dirtiest area first.",
    "Stop cleaning."
)

/**
 * Chat with the robot.
 *
 * Anything that would move the robot comes back as a **pending action** with
 * Confirm / Cancel buttons — the assistant cannot act on its own.
 */
@Composable
fun AssistantScreen(
    onBack: () -> Unit,
    viewModel: AssistantViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🤖 Ask SwachhBot", color = MINT, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row {
                TextButton(onClick = { viewModel.clear() }) {
                    Text("New chat", color = MINT, fontSize = 12.sp)
                }
                TextButton(onClick = onBack) { Text("Back", color = MINT) }
            }
        }

        // Conversation
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onConfirm = { viewModel.confirm(it) },
                    onReject = { viewModel.reject(it) }
                )
            }

            if (busy) {
                item { ThinkingBubble() }
            }

            if (messages.size <= 1) {
                item { SuggestionList(onPick = { viewModel.send(it) }) }
            }
        }

        // Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask me anything…", fontSize = 13.sp) },
                singleLine = true
            )
            Button(
                onClick = {
                    val text = input
                    input = ""
                    viewModel.send(text)
                },
                enabled = input.isNotBlank() && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A5A50))
            ) { Text("Send") }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val bubbleColor = when {
        message.fromUser -> USER_BUBBLE
        message.isError -> Color(0xFF3A2A2A)
        else -> BOT_BUBBLE
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 13.sp
            )

            // Concise reasoning: what the assistant actually looked up.
            if (message.reasoning.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Checked:", color = MINT, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                message.reasoning.forEach { step ->
                    Text("· $step", color = Color(0xFF9FC3B4), fontSize = 10.sp)
                }
            }

            // Pending robot actions — explicit confirmation required.
            message.actions.forEach { action ->
                Spacer(Modifier.height(8.dp))
                ActionCard(action, onConfirm, onReject)
            }
        }
    }
}

@Composable
private fun ActionCard(
    action: AssistantActionUi,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ACTION_CARD)
            .padding(10.dp)
    ) {
        Column {
            Text(action.summary, color = YELLOW, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                text = when (action.status) {
                    "PENDING" -> "Waiting for your confirmation"
                    "EXECUTED" -> "Confirmed and sent to the robot"
                    "REJECTED" -> "Cancelled"
                    "EXPIRED" -> "Expired — ask again"
                    else -> action.status
                },
                color = Color(0xFFBFD9CD),
                fontSize = 10.sp
            )
            if (action.status == "PENDING") {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { onConfirm(action.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Confirm", fontSize = 12.sp) }
                    TextButton(onClick = { onReject(action.id) }) {
                        Text("Cancel", color = Color(0xFFE57373), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BOT_BUBBLE)
                .padding(12.dp)
        ) {
            Text("Thinking…", color = MINT, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SuggestionList(onPick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text("Try asking:", color = MINT, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        SUGGESTIONS.forEach { suggestion ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BOT_BUBBLE)
                        .clickable { onPick(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(suggestion, color = Color(0xFF9FC3B4), fontSize = 12.sp)
                }
            }
        }
    }
}
