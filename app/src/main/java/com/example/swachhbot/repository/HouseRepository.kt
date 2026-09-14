package com.example.swachhbot.repository

import com.example.swachhbot.db.entity.*
import kotlinx.coroutines.flow.Flow

interface HouseRepository {
    suspend fun saveHouse(house: HouseEntity)
    suspend fun getHouse(id: String): HouseEntity?
    fun getRooms(houseId: String): Flow<List<RoomEntity>>
    suspend fun saveRoom(room: RoomEntity)
    
    suspend fun saveObject(obj: ObjectEntity)
    fun getObjects(houseId: String): Flow<List<ObjectEntity>>
    
    suspend fun saveOccupancyGrid(grid: OccupancyGridEntity)
    suspend fun getLatestGrid(houseId: String): OccupancyGridEntity?
    
    suspend fun saveSession(session: CleaningSessionEntity)
    fun getSessions(houseId: String): Flow<List<CleaningSessionEntity>>
    
    suspend fun saveProblemArea(area: ProblemAreaEntity)
    fun getProblemAreas(houseId: String): Flow<List<ProblemAreaEntity>>
}
