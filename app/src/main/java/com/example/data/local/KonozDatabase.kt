package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [KnowledgeRuleEntity::class, AnalysisRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KonozDatabase : RoomDatabase() {
    abstract fun konozDao(): KonozDao

    companion object {
        @Volatile
        private var INSTANCE: KonozDatabase? = null

        fun getInstance(context: Context): KonozDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KonozDatabase::class.java,
                    "konoz_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
