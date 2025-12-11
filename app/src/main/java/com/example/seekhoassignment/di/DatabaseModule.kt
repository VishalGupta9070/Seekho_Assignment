package com.example.seekhoassignment.di

import android.content.Context
import androidx.room.Room
import com.example.seekhoassignment.data.local.AppDatabase
import com.example.seekhoassignment.data.local.dao.AnimeDao
import com.example.seekhoassignment.data.local.dao.AnimeDetailDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "anime_db")
            .build()

    @Provides
    @Singleton
    fun provideAnimeDao(db: AppDatabase): AnimeDao = db.animeDao()

    @Provides
    @Singleton
    fun provideAnimeDetailDao(db: AppDatabase): AnimeDetailDao = db.animeDetailDao()
}

