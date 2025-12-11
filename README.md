SeekhoAssignment — Anime Explorer App

A modern Android application built with Jetpack Compose, Hilt, Room, ViewModel, Coroutines, and WebView-based YouTube playback.
The app fetches anime lists and detailed information from the Jikan API, supports offline usage, and provides auto-sync on network restore.

Features
✅ Anime List Screen

1. Fetches anime list from Jikan API
2. Caches data in Room for offline use
3. Supports Pull-to-refresh
4. Auto-sync when network becomes available
5. Jetpack Compose UI

🎬 Anime Detail Screen

1. Displays full anime details (title, rating, genres, cast, synopsis, etc.)
2. Offline-aware video section
3. If YouTube trailer available → play using in-app WebView YouTube player
4. Auto-sync when network reconnects
5. If offline and no local file → show a connection message
6. Swipe-to-refresh support

🧱 Architecture

1. MVVM (ViewModel + Repository + Room + Compose UI)
2. Clean presentation state (DetailUiState, AnimeListUiState)
3. Lifecycle-safe flows + StateFlow + SharedFlow for events
4. Hilt for dependency injection
5. Separation between Repo, VM, UI, and Presentation Models

🔥 Key Implementations

✔ Extracting YouTube Video ID
Handles:
1.embed URLs
2. watch URLs
3. youtu.be URLs
fun extractYoutubeId(input: String?): String?

✔ WebView YouTube Player with Fullscreen Support
1. Loads custom HTML wrapper
2. Handles fullscreen (onShowCustomView)
YouTubeWebPlayer(embedOrWatchOrId = detail.youtubeId!!)

✔ Auto Sync Logic
AnimeDetailViewModel listens to network changes:
networkMonitor.networkStatusFlow().collect { status ->
    if (status == NetworkStatus.Available) refresh(id)
}

✔ Swipe to Refresh
Detail + List screens both use:
SwipeRefresh(state = swipeState, onRefresh = onRetry)

✔ Decoupled Presentation State
ViewModel → UI mapping ensures UI remains stable and testable:
sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val detail: AnimeDetailEntity) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
