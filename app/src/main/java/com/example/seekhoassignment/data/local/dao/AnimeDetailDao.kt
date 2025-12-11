package com.example.seekhoassignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seekhoassignment.data.local.entities.AnimeDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDetailDao {
    @Query("SELECT * FROM anime_details WHERE mal_id = :id LIMIT 1")
    fun getByIdFlow(id: Int): Flow<AnimeDetailEntity?>

    @Query("SELECT * FROM anime_details WHERE mal_id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Int): AnimeDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AnimeDetailEntity)

    @Query("DELETE FROM anime_details WHERE mal_id = :id")
    suspend fun deleteById(id: Int)
}
