package com.example.seekhoassignment.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.seekhoassignment.data.local.entities.AnimeEntity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class AnimeListUiState {
    object Loading : AnimeListUiState()
    data class Success(val items: List<AnimeEntity>) : AnimeListUiState()
    data class Error(val message: String) : AnimeListUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun animeListScreen(
    state: AnimeListUiState,
    onRefresh: () -> Unit,
    onAnimeClick: (Int) -> Unit,
    events: Flow<String>? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val swipeState = rememberSwipeRefreshState(isRefreshing = false)

    LaunchedEffect(events) {
        events?.collectLatest { message ->
            swipeState.isRefreshing = false
            try {
                snackbarHostState.showSnackbar(message)
            } catch (t: Throwable) {
//                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top Anime") },
                actions = {}
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SwipeRefresh(
                state = swipeState,
                onRefresh = {
                    swipeState.isRefreshing = true
                    onRefresh()
                    if (events == null) {
                        scope.launch {
                            kotlinx.coroutines.delay(1500)
                            swipeState.isRefreshing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when (state) {
                    is AnimeListUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is AnimeListUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Failed to load: ${state.message}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRefresh) { Text("Retry") }
                        }
                    }
                    is AnimeListUiState.Success -> {
                        val animeList = state.items
                        if (animeList.isEmpty()) {
                            Text("No items", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(animeList, key = { it.malId }) { anime ->
                                    animeListRow(item = anime, onClick = onAnimeClick)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun animeListRow(item: AnimeEntity, onClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick(item.malId) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .size(96.dp)
                    .padding(end = 12.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = item.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Episodes: ${item.episodes ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Score: ${item.score ?: "-"}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun sampleAnime(id: Int) = AnimeEntity(
    malId = id,
    title = "Sample Anime #$id",
    synopsis = "Sample synopsis",
    episodes = 12,
    score = 8.5,
    posterUrl = "https://cdn.myanimelist.net/images/anime/1015/138006.jpg",
    lastUpdated = System.currentTimeMillis()
)

@Preview(showBackground = true)
@Composable
fun animeListScreenPreview_Loading() {
    animeListScreen(state = AnimeListUiState.Loading, onRefresh = {}, onAnimeClick = {})
}

@Preview(showBackground = true)
@Composable
fun animeListScreenPreview_Success() {
    val items = remember { List(6) { idx -> sampleAnime(idx + 1) } }
    animeListScreen(state = AnimeListUiState.Success(items), onRefresh = {}, onAnimeClick = {})
}
