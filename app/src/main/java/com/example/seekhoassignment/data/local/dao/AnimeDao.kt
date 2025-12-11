package com.example.seekhoassignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seekhoassignment.data.local.entities.AnimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {

    @Query("SELECT * FROM anime ORDER BY score DESC")
    fun getAll(): Flow<List<AnimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AnimeEntity>)

    @Query("DELETE FROM anime")
    suspend fun clearAll()

}

