package com.example.seekhoassignment.data.repository

import android.util.Log
import com.example.seekhoassignment.data.local.dao.AnimeDao
import com.example.seekhoassignment.data.local.entities.AnimeEntity
import com.example.seekhoassignment.data.remote.apiinterfaces.JikanService
import com.example.seekhoassignment.data.remote.dtos.Anime
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed class RefreshResult {
    object Updated : RefreshResult()
    object NoChange : RefreshResult()
    object Failure : RefreshResult()
}

class AnimeRepository @Inject constructor(
    private val remote: JikanService,
    private val dao: AnimeDao
) {
    private val TAG = "AnimeRepository"

    fun observeTopAnime() = dao.getAll()

    suspend fun refreshTopAnime(page: Int = 1): RefreshResult {
        Log.d(TAG, "refreshTopAnime() called (page=$page)")
        return try {
            val response = remote.getTopAnime(page)
            Log.d(TAG, "network response: code=${response.code()} message=${response.message()}")

            if (!response.isSuccessful || response.body() == null) {
                Log.e(TAG, "Network call unsuccessful: code=${response.code()}")
                return RefreshResult.Failure
            }

            val newEntities = mapResponseToEntities(response.body()!!)

            val old = try {
                dao.getAll().first()
            } catch (e: Exception) {
                Log.d(TAG, "failed to read old data from DB: ${e.message}")
                emptyList()
            }

            if (old.size != newEntities.size) {
                Log.d(TAG, "Size changed (old=${old.size}, new=${newEntities.size}) -> update DB")
                dao.clearAll()
                dao.insertAll(newEntities)
                return RefreshResult.Updated
            }

            val oldMap = old.associateBy { it.malId }
            val newMap = newEntities.associateBy { it.malId }

            // If keys differ -> changed
            if (oldMap.keys != newMap.keys) {
                Log.d(TAG, "ID set changed -> update DB")
                dao.clearAll()
                dao.insertAll(newEntities)
                return RefreshResult.Updated
            }

            // Compare important fields for each id
            val changed = oldMap.any { (id, oldItem) ->
                val newItem = newMap[id]!!

                oldItem.title != newItem.title ||
                        (oldItem.posterUrl ?: "") != (newItem.posterUrl ?: "") ||
                        (oldItem.score ?: -1.0) != (newItem.score ?: -1.0) ||
                        (oldItem.episodes ?: -1) != (newItem.episodes ?: -1)
            }

            return if (changed) {
                Log.d(TAG, "Content changed -> update DB")
                dao.clearAll()
                dao.insertAll(newEntities)
                RefreshResult.Updated
            } else {
                Log.d(TAG, "Data identical — skipping DB write")
                RefreshResult.NoChange
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshTopAnime failed", e)
            RefreshResult.Failure
        }
    }

    private fun mapResponseToEntities(body: Anime): List<AnimeEntity> =
        body.data.mapNotNull { item ->
            val malId = item.mal_id ?: return@mapNotNull null
            val poster = item.images?.jpg?.image_url ?: item.images?.webp?.image_url
            AnimeEntity(
                malId = malId,
                title = item.title ?: item.title_english ?: "Unknown Title",
                synopsis = item.synopsis,
                episodes = item.episodes,
                score = item.score,
                posterUrl = poster,
                lastUpdated = System.currentTimeMillis()
            )
        }
}
