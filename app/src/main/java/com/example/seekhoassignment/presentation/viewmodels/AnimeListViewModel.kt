package com.example.seekhoassignment.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seekhoassignment.data.local.entities.AnimeEntity
import com.example.seekhoassignment.data.repository.AnimeRepository
import com.example.seekhoassignment.data.repository.RefreshResult
import com.example.seekhoassignment.util.NetworkMonitor
import com.example.seekhoassignment.util.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeListViewModel @Inject constructor(
    private val repo: AnimeRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val TAG = "AnimeListViewModel"

    val uiState: StateFlow<UiState>

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    // prevent concurrent refreshes
    private var refreshJob: Job? = null

    init {
        uiState = repo.observeTopAnime()
            .map { list ->
                if (list.isEmpty()) UiState.Loading else UiState.Success(list)
            }
            .catch { emit(UiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

        // Only refresh on cold start if DB is empty
        viewModelScope.launch {
            val current = repo.observeTopAnime().first()
            if (current.isEmpty()) {
                refreshInternal()
            } else {
                Log.d(TAG, "DB already has data — skipping immediate network refresh")
            }
        }

        observeNetworkAndSync()
    }

    fun refresh() {

        if (refreshJob?.isActive == true) {
            Log.d(TAG, "refresh() ignored because a refresh is already running")
            return
        }
        refreshJob = viewModelScope.launch { refreshInternal() }
    }

    private suspend fun refreshInternal(page: Int = 1) {
        Log.d(TAG, "refreshInternal() starting")
        when (repo.refreshTopAnime(page)) {
            RefreshResult.Updated -> {
                Log.d(TAG, "Refresh: Updated")
                _events.tryEmit("List updated")
            }

            RefreshResult.NoChange -> {
                Log.d(TAG, "Refresh: No change")
                _events.tryEmit("No new updates")
            }

            RefreshResult.Failure -> {
                Log.d(TAG, "Refresh: Failure")
                _events.tryEmit("Refresh failed")
            }
        }
    }

    private fun observeNetworkAndSync(page: Int = 1) {
        viewModelScope.launch {

            networkMonitor.networkStatusFlow()
                .drop(1)
                .collect { status ->
                    Log.d(TAG, "Network status changed: $status")
                    if (status == NetworkStatus.Available) {
                        Log.d(TAG, "Network available -> attempting background sync")
                        // run in background; emit events for all outcomes so UI is informed
                        launch {
                            when (repo.refreshTopAnime(page)) {
                                RefreshResult.Updated -> {
                                    Log.d(TAG, "Background sync: Updated")
                                    _events.tryEmit("List updated (synced)")
                                }

                                RefreshResult.NoChange -> {
                                    Log.d(TAG, "Background sync: No change")
                                    _events.tryEmit("No new updates")
                                }

                                RefreshResult.Failure -> {
                                    Log.d(TAG, "Background sync: Failure")
                                    _events.tryEmit("Background sync failed")
                                }
                            }
                        }
                    }
                }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val items: List<AnimeEntity>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
