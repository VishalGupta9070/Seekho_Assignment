package com.example.seekhoassignment.data.repository

import android.util.Log
import com.example.seekhoassignment.data.local.dao.AnimeDetailDao
import com.example.seekhoassignment.data.local.entities.AnimeDetailEntity
import com.example.seekhoassignment.data.remote.apiinterfaces.JikanService
import com.example.seekhoassignment.data.remote.dtos.AnimeDetails
import javax.inject.Inject

sealed class DetailRefreshResult {
    object Updated : DetailRefreshResult()
    object NoChange : DetailRefreshResult()
    object Failure : DetailRefreshResult()
}

class AnimeDetailRepository @Inject constructor(
    private val api: JikanService,
    private val dao: AnimeDetailDao,
) {
    fun observeDetail(id: Int) = dao.getByIdFlow(id)

    suspend fun getDetailOnce(id: Int) = try {
        dao.getByIdOnce(id)
    } catch (t: Throwable) {
        null
    }

    suspend fun fetchAndCacheDetail(id: Int): DetailRefreshResult {
        return try {
            val resp = api.getAnimeDetails(id)
            if (!resp.isSuccessful || resp.body() == null) {
                Log.w("Repo", "API failed: ${resp.code()} ${resp.message()}")
                return DetailRefreshResult.Failure
            }
            Log.d("Repo", "fetchAndCacheDetail: ${resp.body()}")
            val entity = mapToEntity(resp.body()!!)
            val old = try {
                dao.getByIdOnce(id)
            } catch (t: Throwable) {
                null
            }

            if (old == null || old != entity) {

                dao.insert(entity)
                DetailRefreshResult.Updated
            } else DetailRefreshResult.NoChange
        } catch (e: Exception) {
            Log.e("Repo", "fetch failed", e)
            DetailRefreshResult.Failure
        }
    }

    private fun extractYoutubeId(embedUrl: String?): String? {
        if (embedUrl.isNullOrBlank()) return null
        val patterns = listOf(
            Regex("/embed/([A-Za-z0-9_-]{11})"),
            Regex("[?&]v=([A-Za-z0-9_-]{11})"),
            Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
            Regex("/v/([A-Za-z0-9_-]{11})")
        )
        for (re in patterns) {
            val m = re.find(embedUrl)
            if (m != null) return m.groupValues[1]
        }
        val s = embedUrl.trim()
        if (s.length == 11 && s.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return s
        return null
    }

    private fun mapToEntity(dto: AnimeDetails): AnimeDetailEntity {
        val d = dto.data

        val genres = d.genres?.joinToString(", ") { it.name ?: "" }?.takeIf { it.isNotBlank() }
        val cast = d.producers?.joinToString(", ") { it.name ?: "" }?.takeIf { it.isNotBlank() }
        val poster = d.images?.jpg?.image_url ?: d.images?.webp?.image_url

        val youtubeId = extractYoutubeId((d.trailer?.embed_url ?: d.trailer?.url) as String?)
        Log.d("Repos", "mapToEntity: $youtubeId")
        return AnimeDetailEntity(
            malId = d.mal_id ?: 0,
            title = d.title ?: d.title_english ?: "Unknown Title",
            synopsis = d.synopsis?.takeIf { it.isNotBlank() },
            genres = genres,
            cast = cast,
            episodes = d.episodes,
            score = d.score,
            posterUrl = poster,
            youtubeId = youtubeId,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
