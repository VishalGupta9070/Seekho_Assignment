package com.example.seekhoassignment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seekhoassignment.data.local.dao.AnimeDao
import com.example.seekhoassignment.data.local.dao.AnimeDetailDao
import com.example.seekhoassignment.data.local.entities.AnimeDetailEntity
import com.example.seekhoassignment.data.local.entities.AnimeEntity

@Database(
    entities = [AnimeEntity::class, AnimeDetailEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao

    // Add this:
    abstract fun animeDetailDao(): AnimeDetailDao

}

