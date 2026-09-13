package com.example.swachhbot.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.swachhbot.db.dao.*
import com.example.swachhbot.db.entity.*

@Database(
    entities = [
        HouseEntity::class, 
        RoomEntity::class, 
        ObjectEntity::class, 
        OccupancyGridEntity::class, 
        CleaningSessionEntity::class, 
        ProblemAreaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SwachhDatabase : RoomDatabase() {
    abstract fun houseDao(): HouseDao
    abstract fun objectDao(): ObjectDao
    abstract fun mapDao(): MapDao
    abstract fun sessionDao(): SessionDao
    abstract fun problemAreaDao(): ProblemAreaDao

    companion object {
        @Volatile
        private var INSTANCE: SwachhDatabase? = null

        fun getDatabase(context: Context): SwachhDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwachhDatabase::class.java,
                    "swachh_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
