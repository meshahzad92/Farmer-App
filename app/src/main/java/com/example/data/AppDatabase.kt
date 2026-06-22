package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FertilizerType::class, SalesRecord::class, Farmer::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fertilizerDao(): FertilizerDao
    abstract fun salesDao(): SalesDao
    abstract fun farmerDao(): FarmerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fertilizer_tracker_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default values when DB is created
                        db.execSQL("INSERT INTO fertilizer_types (name, isDefault) VALUES ('626', 1)")
                        db.execSQL("INSERT INTO fertilizer_types (name, isDefault) VALUES ('7468', 1)")
                        db.execSQL("INSERT INTO fertilizer_types (name, isDefault) VALUES ('DAP', 1)")
                        db.execSQL("INSERT INTO fertilizer_types (name, isDefault) VALUES ('Urea', 1)")
                        db.execSQL("INSERT INTO fertilizer_types (name, isDefault) VALUES ('SOP', 1)")
                        db.execSQL("INSERT INTO fertilizer_types (name, isDefault) VALUES ('NP', 1)")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
