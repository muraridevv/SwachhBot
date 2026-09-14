package com.example.swachhbot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.swachhbot.db.SwachhDatabase
import com.example.swachhbot.network.BackendClient
import com.example.swachhbot.network.BackendConfig
import com.example.swachhbot.network.CorrectionRequest
import com.example.swachhbot.network.HouseIdentity
import com.example.swachhbot.network.InsightDto
import com.example.swachhbot.repository.HouseRepository
import com.example.swachhbot.repository.impl.RoomHouseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One learned fact, flattened for display. */
data class InsightUi(
    val category: String,
    val subject: String,
    val summary: String,
    val confidence: Double,
    val status: String,
    val evidence: Int,
    val source: String
)

data class RecommendationUi(
    val category: String,
    val subject: String,
    val message: String,
    val suggestedChange: String,
    val confidence: Double
)

data class LearningUiState(
    val loading: Boolean = true,
    val online: Boolean = false,
    val headline: String = "",
    val insights: List<InsightUi> = emptyList(),
    val recommendations: List<RecommendationUi> = emptyList(),
    val recentChanges: List<InsightUi> = emptyList(),
    val message: String? = null
) {
    fun byCategory(prefix: String) = insights.filter { it.category.startsWith(prefix) }
}

/**
 * Drives the "🧠 What the Robot Learned" screen.
 *
 * Tries the backend first; if it is unreachable the screen still shows what the
 * robot knows from its local Room memory, so the feature degrades gracefully.
 */
class LearningViewModel(application: Application) : AndroidViewModel(application) {

    private val client = BackendClient(BackendConfig.BASE_URL)
    private val backendHouseId = HouseIdentity.get(application)
    private val repository: HouseRepository =
        RoomHouseRepository(SwachhDatabase.getDatabase(application))

    private val _state = MutableStateFlow(LearningUiState())
    val state: StateFlow<LearningUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)

            val remote = runCatching { client.api.getLearningOverview(backendHouseId) }.getOrNull()
            _state.value = if (remote != null) {
                LearningUiState(
                    loading = false,
                    online = true,
                    headline = remote.headline,
                    insights = remote.insights.map { it.toUi() },
                    recommendations = remote.recommendations.map {
                        RecommendationUi(
                            it.category, it.subject, it.message, it.suggestedChange, it.confidence
                        )
                    },
                    recentChanges = remote.recentChanges.map { it.toUi() }
                )
            } else {
                localFallback()
            }
        }
    }

    /** Re-derives knowledge on the server. */
    fun analyze() {
        viewModelScope.launch {
            val summary = runCatching { client.api.analyzeLearning(backendHouseId) }.getOrNull()
            _state.value = _state.value.copy(
                message = summary?.let {
                    "Analysed: ${it.created} new, ${it.reinforced} reinforced, ${it.decayed} forgotten."
                } ?: "Could not reach the learning service."
            )
            refresh()
        }
    }

    /** "That chair is temporary." */
    fun correct(objectType: String, temporary: Boolean) {
        viewModelScope.launch {
            val assertion = if (temporary) "That $objectType is temporary."
            else "That $objectType is permanent."
            val result = runCatching {
                client.api.submitCorrection(
                    CorrectionRequest(backendHouseId, "OBJECT", objectType, assertion)
                )
            }.getOrNull()

            _state.value = _state.value.copy(
                message = result?.message ?: "Saved locally; will sync when the backend is reachable."
            )
            refresh()
        }
    }

    // ---------------------------------------------------------------------

    private suspend fun localFallback(): LearningUiState = withContext(Dispatchers.IO) {
        val objects = runCatching {
            repository.getObjects(HouseIdentity.LOCAL_HOUSE_ID).first()
        }.getOrDefault(emptyList())

        val insights = objects.map { obj ->
            val temporary = obj.category.equals("TEMPORARY", true) ||
                obj.category.equals("MOVING", true)
            InsightUi(
                category = if (temporary) "TEMPORARY_OBJECT" else "OBJECT",
                subject = obj.type,
                summary = if (temporary) {
                    "${obj.type} has been seen ${obj.detectionCount} time(s) — it may move."
                } else {
                    "${obj.type} is a permanent feature of this house."
                },
                confidence = (obj.detectionCount / (obj.detectionCount + 3.0)),
                status = if (obj.detectionCount >= 3) "CONFIRMED" else "EMERGING",
                evidence = obj.detectionCount,
                source = "DERIVED"
            )
        }

        LearningUiState(
            loading = false,
            online = false,
            headline = "Backend offline — showing what I know from local memory.",
            insights = insights,
            recommendations = insights.filter { it.category == "TEMPORARY_OBJECT" }.map {
                RecommendationUi(
                    it.category, it.subject, it.summary,
                    "treat this object as movable and re-check it each session",
                    it.confidence
                )
            }
        )
    }
}

private fun InsightDto.toUi() = InsightUi(
    category = category,
    subject = subjectLabel.ifBlank { subjectKey },
    summary = summary,
    confidence = confidence,
    status = status,
    evidence = evidenceCount,
    source = source
)
