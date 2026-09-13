package com.example.swachhbot.db.dao

import androidx.room.*
import com.example.swachhbot.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouse(house: HouseEntity)

    @Query("SELECT * FROM houses WHERE id = :id")
    suspend fun getHouseById(id: String): HouseEntity?

    @Transaction
    @Query("SELECT * FROM rooms WHERE houseId = :houseId")
    fun getRoomsForHouse(houseId: String): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)
}

@Dao
interface ObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(obj: ObjectEntity)

    @Query("SELECT * FROM detected_objects WHERE houseId = :houseId")
    fun getObjectsForHouse(houseId: String): Flow<List<ObjectEntity>>

    @Query("SELECT * FROM detected_objects WHERE id = :id")
    suspend fun getObjectById(id: String): ObjectEntity?
}

@Dao
interface MapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccupancyGrid(grid: OccupancyGridEntity)

    @Query("SELECT * FROM occupancy_grids WHERE houseId = :houseId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestGridForHouse(houseId: String): OccupancyGridEntity?
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CleaningSessionEntity)

    @Query("SELECT * FROM cleaning_sessions WHERE houseId = :houseId ORDER BY timestamp DESC")
    fun getSessionsForHouse(houseId: String): Flow<List<CleaningSessionEntity>>
}

@Dao
interface ProblemAreaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblemArea(area: ProblemAreaEntity)

    @Query("SELECT * FROM problem_areas WHERE houseId = :houseId")
    fun getProblemAreasForHouse(houseId: String): Flow<List<ProblemAreaEntity>>
}
