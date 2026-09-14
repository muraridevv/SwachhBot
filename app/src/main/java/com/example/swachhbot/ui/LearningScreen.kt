package com.example.swachhbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.ui.theme.RobotMint
import com.example.swachhbot.ui.theme.StatusOnline
import com.example.swachhbot.ui.theme.StatusPaused
import com.example.swachhbot.viewmodel.InsightUi
import com.example.swachhbot.viewmodel.LearningViewModel
import com.example.swachhbot.viewmodel.RecommendationUi

/**
 * "🧠 What the Robot Learned" — redesigned for Phase 21 Modernization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    onBack: () -> Unit,
    viewModel: LearningViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("House Knowledge", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Headline / status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (state.loading) "Reviewing experiences…" else state.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (state.online) "Live from robot's memory" else "Offline mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.online) StatusOnline else StatusPaused
                    )
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.analyze() },
                    modifier = Modifier.weight(1f)
                ) { 
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-analyze") 
                }
                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.weight(1f)
                ) { 
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh") 
                }
            }

            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            // ---- Recommendations ------------------------------------------------
            if (state.recommendations.isNotEmpty()) {
                SectionTitle("Smart Recommendations")
                state.recommendations.forEach { RecommendationCard(it) }
            }

            // ---- Learned objects ------------------------------------------------
            InsightSection(
                title = "Detected Objects",
                insights = state.insights.filter { it.category == "TEMPORARY_OBJECT" || it.category == "OBJECT" },
                emptyText = "No objects learned yet.",
                onCorrection = { insight, temporary -> viewModel.correct(insight.subject, temporary) }
            )

            // ---- Learned rooms --------------------------------------------------
            InsightSection(
                title = "Room Habits",
                insights = state.insights.filter {
                    it.category == "DIRTY_AREA" || it.category == "ROOM_CLEANLINESS"
                },
                emptyText = "No room habits learned yet."
            )

            // ---- Problematic areas ---------------------------------------------
            InsightSection(
                title = "Trouble Spots",
                insights = state.insights.filter { it.category == "BLOCKED_AREA" },
                emptyText = "No trouble spots reported so far. 🎉"
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun InsightSection(
    title: String,
    insights: List<InsightUi>,
    emptyText: String,
    onCorrection: ((InsightUi, Boolean) -> Unit)? = null
) {
    SectionTitle(title)
    if (insights.isEmpty()) {
        Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        return
    }
    insights.forEach { InsightCard(it, onCorrection) }
}

@Composable
private fun InsightCard(insight: InsightUi, onCorrection: ((InsightUi, Boolean) -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(insight.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                StatusChip(insight.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(insight.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            ConfidenceBar(insight.confidence)
            Spacer(Modifier.height(4.dp))
            Text(
                "Confidence ${(insight.confidence * 100).toInt()}% · ${insight.evidence} evidence(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            if (onCorrection != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onCorrection(insight, true) }) {
                        Text("It's temporary", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { onCorrection(insight, false) }) {
                        Text("It's permanent", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: RecommendationUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(recommendation.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(recommendation.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.height(8.dp))
            Text("Suggested: ${recommendation.suggestedChange}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "CONFIRMED", "USER_CONFIRMED" -> StatusOnline
        "USER_REJECTED" -> Color.Red
        else -> StatusPaused
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.lowercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun ConfidenceBar(confidence: Double) {
    LinearProgressIndicator(
        progress = { confidence.toFloat() },
        modifier = Modifier.fillMaxWidth().height(4.dp),
        strokeCap = StrokeCap.Round
    )
}
