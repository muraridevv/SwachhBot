package com.example.swachhbot.network

import retrofit2.http.*

/**
 * Retrofit definition of the SwachhBot backend REST API.
 *
 * Base URL is typically http://10.0.2.2:8080/ (emulator loopback) or the
 * Raspberry Pi's LAN address in production.
 */
interface SwachhBotApi {

    // ----- Houses -----

    @GET("api/houses")
    suspend fun getHouses(): List<HouseDto>

    @GET("api/houses/{id}")
    suspend fun getHouse(@Path("id") id: String): HouseDto

    @POST("api/houses")
    suspend fun createHouse(@Body house: HouseRequest): HouseDto

    @POST("api/houses/{houseId}/rooms")
    suspend fun addRoom(@Path("houseId") houseId: String, @Body request: RoomRequest): RoomDto

    // ----- Map -----

    @GET("api/houses/{houseId}/map")
    suspend fun getMap(@Path("houseId") houseId: String): MapDto

    @PUT("api/houses/{houseId}/map")
    suspend fun saveMap(
        @Path("houseId") houseId: String,
        @Body request: MapUpdateRequest
    ): MapDto

    // ----- Objects -----

    @GET("api/houses/{houseId}/objects")
    suspend fun getObjects(@Path("houseId") houseId: String): List<ObjectDto>

    @PUT("api/houses/{houseId}/objects")
    suspend fun upsertObject(
        @Path("houseId") houseId: String,
        @Body objectDto: ObjectDto
    ): ObjectDto

    // ----- Robot state -----

    @GET("api/robots/{robotId}/state")
    suspend fun getRobotState(@Path("robotId") robotId: String): RobotStateDto

    @PUT("api/robots/{robotId}/state")
    suspend fun pushRobotState(
        @Path("robotId") robotId: String,
        @Body state: RobotStateDto
    ): RobotStateDto

    // ----- Cleaning sessions -----

    @GET("api/houses/{houseId}/sessions")
    suspend fun getSessions(@Path("houseId") houseId: String): List<SessionDto>

    @POST("api/houses/{houseId}/sessions")
    suspend fun createSession(
        @Path("houseId") houseId: String,
        @Body session: SessionDto
    ): SessionDto

    // ----- Problem areas -----

    @GET("api/houses/{houseId}/problems")
    suspend fun getProblems(@Path("houseId") houseId: String): List<ProblemAreaDto>

    @POST("api/houses/{houseId}/problems")
    suspend fun reportProblem(
        @Path("houseId") houseId: String,
        @Body problem: ProblemAreaDto
    ): ProblemAreaDto

    // ----- Adaptive learning (Phase 10) -----

    @GET("api/learning/overview")
    suspend fun getLearningOverview(@Query("houseId") houseId: String): LearningOverviewDto

    @POST("api/learning/analyze")
    suspend fun analyzeLearning(@Query("houseId") houseId: String): RefreshSummaryDto

    @POST("api/learning/corrections")
    suspend fun submitCorrection(@Body request: CorrectionRequest): CorrectionResultDto

    // ----- AI planning (Phase 9) -----

    @POST("api/ai/plan")
    suspend fun requestPlan(@Body request: PlanRequest): PlanResponse

    // ----- AI assistant (Phase 11) -----

    @POST("api/assistant/chat")
    suspend fun assistantChat(@Body request: AssistantChatRequest): AssistantChatResponse

    @POST("api/assistant/actions/{actionId}/confirm")
    suspend fun confirmAssistantAction(
        @Path("actionId") actionId: String,
        @Query("robotId") robotId: String
    ): AssistantActionDto

    @POST("api/assistant/actions/{actionId}/reject")
    suspend fun rejectAssistantAction(@Path("actionId") actionId: String): AssistantActionDto

    // ----- Exploration (Phase 16) -----
    
    @POST("api/exploration/start")
    suspend fun startExploration()

    @POST("api/exploration/stop")
    suspend fun stopExploration()

    // ----- Commands (Phase 12+) -----

    @GET("api/commands")
    suspend fun getPendingCommands(@Query("robotId") robotId: String): List<CommandDto>

    @POST("api/commands/{id}/ack")
    suspend fun acknowledgeCommand(@Path("id") id: String, @Body request: CommandAckRequest): CommandDto
}

data class PlanRequest(val houseId: String, val request: String)

data class PlanResponse(
    val plan: Map<String, Any>? = null,
    val explanation: Map<String, Any>? = null
)

data class CommandDto(
    val id: String,
    val robotId: String,
    val houseId: String?,
    val command: String,
    val status: String,
    val payload: String?,
    val issuedAt: String,
    val ackedAt: String?
)

data class CommandAckRequest(val status: String)
