package com.example.swachhbot.network

/** Mirrors of the backend assistant DTOs (Phase 11). */

data class AssistantChatRequest(
    val houseId: String,
    val robotId: String,
    val conversationId: String?,
    val message: String
)

data class AssistantChatResponse(
    val conversationId: String? = null,
    val reply: String = "",
    val reasoning: List<ReasoningStepDto> = emptyList(),
    val pendingActions: List<AssistantActionDto> = emptyList(),
    val at: String? = null
)

data class ReasoningStepDto(
    val tool: String = "",
    val note: String = ""
)

data class AssistantActionDto(
    val id: String = "",
    val actionType: String = "",
    val status: String = "",
    val summary: String = "",
    val planId: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null
)
