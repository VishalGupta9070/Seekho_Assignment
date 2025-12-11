package com.example.seekhoassignment.data.remote.apiinterfaces

import com.example.seekhoassignment.data.remote.dtos.Anime
import com.example.seekhoassignment.data.remote.dtos.AnimeDetails
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanService {

    @GET("top/anime")
    suspend fun getTopAnime(@Query("page") page: Int = 1): Response<Anime>

    @GET("anime/{id}")
    suspend fun getAnimeDetails(@Path("id") animeId: Int): Response<AnimeDetails>
}
