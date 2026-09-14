package com.example.swachhbot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.ui.theme.StatusOnline
import com.example.swachhbot.viewmodel.AssistantActionUi
import com.example.swachhbot.viewmodel.AssistantViewModel
import com.example.swachhbot.viewmodel.ChatMessage

private val SUGGESTIONS = listOf(
    "Clean the kitchen.",
    "Don't clean the bedroom.",
    "Where did you get stuck yesterday?",
    "What did you learn about my house?",
    "Which room gets dirty most often?",
    "Clean the dirtiest area first.",
    "Stop cleaning."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    onBack: () -> Unit = {},
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.clear() }) {
                        Text("Clear Chat")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask me anything…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    IconButton(
                        onClick = {
                            val text = input
                            input = ""
                            viewModel.send(text)
                        },
                        enabled = input.isNotBlank() && !busy,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val isUser = message.fromUser
    
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (message.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (message.isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium)

                if (message.reasoning.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("System Thinking:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    message.reasoning.forEach { step ->
                        Text(
                            text = "• $step", 
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Pending robot actions
        message.actions.forEach { action ->
            Spacer(Modifier.height(8.dp))
            ActionCard(action, onConfirm, onReject)
        }
    }
}

@Composable
private fun ActionCard(
    action: AssistantActionUi,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(action.summary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = when (action.status) {
                    "PENDING" -> "Action requires your confirmation"
                    "EXECUTED" -> "Command sent to robot"
                    "REJECTED" -> "Action cancelled"
                    else -> action.status
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            
            if (action.status == "PENDING") {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConfirm(action.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOnline)
                    ) { Text("Confirm") }
                    TextButton(
                        onClick = { onReject(action.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    ) {
        Text(
            "Thinking…", 
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SuggestionList(onPick: (String) -> Unit) {
    Column {
        Text(
            "Quick commands:", 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SUGGESTIONS.forEach { suggestion ->
            SuggestionChip(
                onClick = { onPick(suggestion) },
                label = { Text(suggestion) },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
