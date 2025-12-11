package com.example.seekhoassignment.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seekhoassignment.data.local.entities.AnimeDetailEntity
import com.example.seekhoassignment.data.repository.AnimeDetailRepository
import com.example.seekhoassignment.data.repository.DetailRefreshResult
import com.example.seekhoassignment.util.NetworkMonitor
import com.example.seekhoassignment.util.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val repo: AnimeDetailRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var currentId: Int? = null
    private var collectJob: Job? = null
    private var networkJob: Job? = null

    fun load(id: Int) {
        if (currentId == id && collectJob?.isActive == true) return
        currentId = id

        // Observe DB flow
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repo.observeDetail(id)
                .catch { ex ->
                    _uiState.value = UiState.Error("Failed reading cache: ${ex.message}")
                }
                .collect { entity ->
                    if (entity == null) _uiState.value = UiState.Loading
                    else _uiState.value = UiState.Success(entity)
                }
        }

        // trigger a network refresh once
        refresh(id)

        // auto-sync on network available while user is on this screen
        networkJob?.cancel()
        networkJob = viewModelScope.launch {
            networkMonitor.networkStatusFlow().collect { status ->
                if (status == NetworkStatus.Available) {
                    currentId?.let { refresh(it) }
                }
            }
        }
    }

    fun refresh(id: Int) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (repo.fetchAndCacheDetail(id)) {
                    DetailRefreshResult.Updated -> _events.tryEmit("Detail updated")
                    DetailRefreshResult.NoChange -> _events.tryEmit("No new updates")
                    DetailRefreshResult.Failure -> _events.tryEmit("Refresh failed")
                }
            } catch (t: Throwable) {
                _events.tryEmit("Refresh error: ${t.message}")
            } finally {
                kotlinx.coroutines.delay(200)
                _isRefreshing.value = false
            }
        }
    }

    suspend fun getCachedOnce(id: Int): AnimeDetailEntity? = repo.getDetailOnce(id)

    sealed class UiState {
        object Loading : UiState()
        data class Success(val detail: AnimeDetailEntity) : UiState()
        data class Error(val msg: String) : UiState()
    }
}
