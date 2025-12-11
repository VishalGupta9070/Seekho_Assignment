package com.example.seekhoassignment.presentation.screens

import androidx.compose.foundation.background
import com.example.seekhoassignment.presentation.component.YouTubeWebPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.seekhoassignment.data.local.entities.AnimeDetailEntity


sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val detail: AnimeDetailEntity) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun animeDetailScreen(
    viewState: DetailUiState,
    events: Flow<String>?,
    isRefreshing: Boolean = false,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    getLocalFile: (Int) -> File? = { null },
    isOnlineChecker: (() -> Boolean)? = null,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnlineFinal: () -> Boolean = isOnlineChecker ?: { isOnline(ctx) }

    LaunchedEffect(events) {
        events?.collectLatest { msg ->
            try {
                snackbarHostState.showSnackbar(msg)
            } catch (_: Throwable) {
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Anime Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {}
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (viewState) {
                is DetailUiState.Loading -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                    )
                }

                is DetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).fillMaxSize().background(Color.Gray),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load: ${viewState.message}")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }

                is DetailUiState.Success -> {
                    val detail = viewState.detail
                    val swipeState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
                    SwipeRefresh(state = swipeState, onRefresh = onRetry) {
                        // Use new DetailContent below
                        detailContent(
                            detail = detail,
                            getLocalFile = getLocalFile,
                            isOnlineChecker = isOnlineFinal
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun detailContent(
    detail: AnimeDetailEntity,
    getLocalFile: (Int) -> File?,
    isOnlineChecker: () -> Boolean,
) {
    val scroll = rememberScrollState()
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        val localFile = remember(detail.malId) { getLocalFile(detail.malId) }
        val isOnline = isOnlineChecker()

        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            when {
                localFile != null -> {
                    // TODO: Replace with ExoPlayer to play local files; placeholder shows poster
                    AsyncImage(
                        model = localFile,
                        contentDescription = detail.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // If we have an extracted youtubeId (saved in entity) -> use WebView player
                !detail.youtubeId.isNullOrBlank() -> {
                    // play embedded youtube via WebView
                    YouTubeWebPlayer(
                        embedOrWatchOrId = detail.youtubeId!!,
                        modifier = Modifier.fillMaxSize()
                    )

                }

                else -> {
                    // No trailer available showing poster
                    AsyncImage(
                        model = detail.posterUrl,
                        contentDescription = detail.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // If trailer exists but offline and no local file, showing message overlay
            if (!isOnline && !detail.youtubeId.isNullOrBlank() && localFile == null) {
                Text(
                    "Connect to internet to play trailer",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            detail.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Episodes: ${detail.episodes ?: "-"}", style = MaterialTheme.typography.bodyMedium)
            Text("Score: ${detail.score ?: "-"}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(8.dp))

        detail.genres?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall
            ); Spacer(modifier = Modifier.height(8.dp))
        }
        detail.cast?.let {
            Text("Cast: $it", style = MaterialTheme.typography.bodySmall); Spacer(
            modifier = Modifier.height(8.dp)
        )
        }

        Text("Synopsis", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            detail.synopsis ?: "No synopsis available",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun isOnline(context: android.content.Context): Boolean {
    try {
        val cm =
            context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    } catch (_: Throwable) {
        return false
    }
}
