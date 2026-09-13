package com.example.swachhbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swachhbot.viewmodel.InsightUi
import com.example.swachhbot.viewmodel.LearningViewModel
import com.example.swachhbot.viewmodel.RecommendationUi

private val BACKGROUND = Color(0xFF142420)
private val MINT = Color(0xFFB8FFCF)
private val CARD = Color(0xFF1E332C)
private val YELLOW = Color(0xFFFFD166)
private val RED = Color(0xFFFF6B6B)

/**
 * "🧠 What the Robot Learned" — everything the adaptive learning layer knows
 * about this house, with confidence levels, recent changes and user corrections.
 */
@Composable
fun LearningScreen(
    onBack: () -> Unit,
    viewModel: LearningViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND)
    ) {
        // Header stays pinned while the content scrolls.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🧠 What the Robot Learned",
                color = MINT,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = onBack) { Text("Back", color = MINT) }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

        Spacer(Modifier.height(8.dp))

        // Headline / status
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CARD)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = if (state.loading) "Reviewing my experiences…" else state.headline,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.online) "Live from the robot's memory (backend connected)"
                    else "Offline — using local memory",
                    color = if (state.online) MINT else YELLOW,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.analyze() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A5A50))
            ) { Text("Re-analyze") }
            Button(
                onClick = { viewModel.refresh() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A5A50))
            ) { Text("Refresh") }
        }

        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MINT, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        // ---- Learned objects ------------------------------------------------
        InsightSection(
            title = "Learned objects",
            insights = state.insights.filter { it.category == "TEMPORARY_OBJECT" || it.category == "OBJECT" },
            emptyText = "No objects learned yet. Turn on Vision and let the robot explore.",
            onCorrection = { insight, temporary -> viewModel.correct(insight.subject, temporary) }
        )

        // ---- Learned rooms --------------------------------------------------
        InsightSection(
            title = "Learned rooms",
            insights = state.insights.filter {
                it.category == "DIRTY_AREA" || it.category == "ROOM_CLEANLINESS"
            },
            emptyText = "No room habits learned yet."
        )

        // ---- Problematic areas ---------------------------------------------
        InsightSection(
            title = "Problematic areas",
            insights = state.insights.filter { it.category == "BLOCKED_AREA" },
            emptyText = "No trouble spots reported so far. 🎉"
        )

        // ---- Cleaning patterns ---------------------------------------------
        InsightSection(
            title = "Cleaning patterns",
            insights = state.insights.filter { it.category == "CLEANING_PATTERN" },
            emptyText = "Not enough sessions yet to spot a pattern."
        )

        // ---- Recent changes -------------------------------------------------
        InsightSection(
            title = "Recent changes",
            insights = state.recentChanges,
            emptyText = "The house layout looks unchanged."
        )

        // ---- Recommendations ------------------------------------------------
        if (state.recommendations.isNotEmpty()) {
            SectionTitle("Recommendations")
            state.recommendations.forEach { RecommendationCard(it) }
        }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MINT,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
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
        Text(emptyText, color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        return
    }
    insights.forEach { InsightCard(it, onCorrection) }
}

@Composable
private fun InsightCard(insight: InsightUi, onCorrection: ((InsightUi, Boolean) -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CARD)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(insight.subject, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                StatusChip(insight.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(insight.summary, color = Color(0xFFCFE5DC), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            ConfidenceBar(insight.confidence)
            Text(
                "confidence ${(insight.confidence * 100).toInt()}% · ${insight.evidence} observation(s) · ${insight.source.lowercase()}",
                color = Color.Gray,
                fontSize = 10.sp
            )

            if (onCorrection != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onCorrection(insight, true) }) {
                        Text("That's temporary", color = YELLOW, fontSize = 11.sp)
                    }
                    TextButton(onClick = { onCorrection(insight, false) }) {
                        Text("That's permanent", color = MINT, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: RecommendationUi) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1B3A32))
            .padding(12.dp)
    ) {
        Column {
            Text(recommendation.subject, color = MINT, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(recommendation.message, color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("→ ${recommendation.suggestedChange}", color = YELLOW, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "CONFIRMED", "USER_CONFIRMED" -> MINT
        "USER_REJECTED" -> RED
        else -> YELLOW
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(status.replace('_', ' ').lowercase(), color = color, fontSize = 10.sp)
    }
}

/** Simple hand-rolled bar so we do not depend on a specific Compose version's API. */
@Composable
private fun ConfidenceBar(confidence: Double) {
    val fraction = confidence.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0E1B17))
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MINT)
            )
        }
    }
}
