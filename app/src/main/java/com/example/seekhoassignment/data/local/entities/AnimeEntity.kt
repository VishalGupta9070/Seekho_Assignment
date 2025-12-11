package com.example.seekhoassignment.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime")
data class AnimeEntity(
    @PrimaryKey
    @ColumnInfo(name = "mal_id")
    val malId: Int,
    val title: String,
    val synopsis: String? = null,
    val episodes: Int? = null,
    val score: Double? = null,
    @ColumnInfo(name = "poster_url")
    val posterUrl: String? = null,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
