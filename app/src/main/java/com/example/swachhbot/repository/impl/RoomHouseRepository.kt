package com.example.swachhbot.repository.impl

import com.example.swachhbot.db.SwachhDatabase
import com.example.swachhbot.db.entity.*
import com.example.swachhbot.repository.HouseRepository
import kotlinx.coroutines.flow.Flow

class RoomHouseRepository(private val db: SwachhDatabase) : HouseRepository {
    override suspend fun saveHouse(house: HouseEntity) = db.houseDao().insertHouse(house)
    override suspend fun getHouse(id: String): HouseEntity? = db.houseDao().getHouseById(id)
    override fun getRooms(houseId: String): Flow<List<RoomEntity>> = db.houseDao().getRoomsForHouse(houseId)
    override suspend fun saveRoom(room: RoomEntity) = db.houseDao().insertRoom(room)

    override suspend fun saveObject(obj: ObjectEntity) = db.objectDao().insertObject(obj)
    override fun getObjects(houseId: String): Flow<List<ObjectEntity>> = db.objectDao().getObjectsForHouse(houseId)

    override suspend fun saveOccupancyGrid(grid: OccupancyGridEntity) = db.mapDao().insertOccupancyGrid(grid)
    override suspend fun getLatestGrid(houseId: String): OccupancyGridEntity? = db.mapDao().getLatestGridForHouse(houseId)

    override suspend fun saveSession(session: CleaningSessionEntity) = db.sessionDao().insertSession(session)
    override fun getSessions(houseId: String): Flow<List<CleaningSessionEntity>> = db.sessionDao().getSessionsForHouse(houseId)

    override suspend fun saveProblemArea(area: ProblemAreaEntity) = db.problemAreaDao().insertProblemArea(area)
    override fun getProblemAreas(houseId: String): Flow<List<ProblemAreaEntity>> = db.problemAreaDao().getProblemAreasForHouse(houseId)
}
