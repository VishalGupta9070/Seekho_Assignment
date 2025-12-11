package com.example.seekhoassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.seekhoassignment.presentation.screens.*
import com.example.seekhoassignment.presentation.viewmodels.AnimeDetailViewModel
import com.example.seekhoassignment.presentation.viewmodels.AnimeListViewModel
import com.example.seekhoassignment.presentation.screens.AnimeListUiState
import com.example.seekhoassignment.presentation.screens.animeListScreen
import com.example.seekhoassignment.ui.theme.SeekhoAssignmentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeekhoAssignmentTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            val vm: AnimeListViewModel = hiltViewModel()
                            val vmState by vm.uiState.collectAsState()

                            val uiState = when (val s = vmState) {
                                is AnimeListViewModel.UiState.Loading -> AnimeListUiState.Loading
                                is AnimeListViewModel.UiState.Error -> AnimeListUiState.Error(s.message)
                                is AnimeListViewModel.UiState.Success -> AnimeListUiState.Success(s.items)
                                else -> AnimeListUiState.Loading
                            }

                            animeListScreen(
                                state = uiState,
                                onRefresh = { vm.refresh() },
                                onAnimeClick = { animeId -> navController.navigate("detail/$animeId") },
                                events = vm.events
                            )
                        }

                        composable("detail/{id}") { backStack ->
                            val id = backStack.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                            val vm: AnimeDetailViewModel = hiltViewModel(backStack)

                            LaunchedEffect(id) { vm.load(id) }

                            val vmState by vm.uiState.collectAsState()
                            val events = vm.events
                            val refreshing by vm.isRefreshing.collectAsState()

                            val viewState: DetailUiState = when (vmState) {
                                is AnimeDetailViewModel.UiState.Loading -> DetailUiState.Loading
                                is AnimeDetailViewModel.UiState.Error -> DetailUiState.Error((vmState as AnimeDetailViewModel.UiState.Error).msg)
                                is AnimeDetailViewModel.UiState.Success -> DetailUiState.Success((vmState as AnimeDetailViewModel.UiState.Success).detail)
                                else -> DetailUiState.Loading
                            }

                            val getLocalFile: (Int) -> java.io.File? = { null }

                            animeDetailScreen(
                                viewState = viewState,
                                events = events,
                                isRefreshing = refreshing,
                                onRetry = { vm.refresh(id) },
                                onBack = { navController.popBackStack() },
                                getLocalFile = getLocalFile
                            )
                        }
                    }
                }
            }
        }
    }
}
