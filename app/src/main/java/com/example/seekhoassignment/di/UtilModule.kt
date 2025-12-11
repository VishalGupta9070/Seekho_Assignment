package com.example.seekhoassignment.di

import android.content.Context
import com.example.seekhoassignment.util.NetworkMonitor
import com.example.seekhoassignment.util.NetworkMonitorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        impl: NetworkMonitorImpl
    ): NetworkMonitor
}
