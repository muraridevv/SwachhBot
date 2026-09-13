package com.example.swachhbot.network

/** Mirrors of the backend adaptive-learning DTOs. */

data class InsightDto(
    val id: String? = null,
    val category: String = "",
    val subjectKey: String = "",
    val subjectLabel: String = "",
    val summary: String = "",
    val confidence: Double = 0.0,
    val evidenceCount: Int = 0,
    val status: String = "",
    val source: String = "",
    val details: String? = null,
    val lastUpdatedAt: String? = null
)

data class RecommendationDto(
    val category: String = "",
    val subject: String = "",
    val message: String = "",
    val suggestedChange: String = "",
    val confidence: Double = 0.0,
    val status: String = ""
)

data class LearningOverviewDto(
    val houseId: String? = null,
    val headline: String = "",
    val insights: List<InsightDto> = emptyList(),
    val recommendations: List<RecommendationDto> = emptyList(),
    val recentChanges: List<InsightDto> = emptyList(),
    val confirmedCount: Int = 0,
    val emergingCount: Int = 0,
    val generatedAt: String? = null
)

data class CorrectionRequest(
    val houseId: String,
    val targetType: String,
    val targetKey: String,
    val assertion: String
)

data class CorrectionResultDto(
    val applied: Boolean = false,
    val message: String = "",
    val affected: List<String> = emptyList()
)

data class RefreshSummaryDto(
    val created: Int = 0,
    val reinforced: Int = 0,
    val decayed: Int = 0,
    val skipped: Int = 0,
    val observed: Int = 0
)
