package com.waheed.universaldownloader.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.waheed.universaldownloader.data.local.DownloadEntity
import com.waheed.universaldownloader.ui.components.GlassCard
import com.waheed.universaldownloader.ui.navigation.NavRoutes
import com.waheed.universaldownloader.ui.theme.AmberPrimary
import com.waheed.universaldownloader.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    navController: NavHostController,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (state.isSelectionMode) {
                            Text("${state.selectedIds.size} selected")
                        } else {
                            Text("Library")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.isSelectionMode) viewModel.exitSelectionMode()
                            else navController.popBackStack()
                        }) {
                            Icon(
                                if (state.isSelectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (state.isSelectionMode) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                            }
                            IconButton(onClick = { viewModel.deleteSelected() }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    if (state.viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                                    contentDescription = "Toggle view"
                                )
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    SortMode.entries.forEach { sort ->
                                        DropdownMenuItem(
                                            text = { Text(sort.label) },
                                            onClick = {
                                                viewModel.setSortMode(sort)
                                                showSortMenu = false
                                            },
                                            leadingIcon = {
                                                if (state.sortMode == sort) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AmberPrimary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                if (!state.isSelectionMode) {
                    // Search bar
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search downloads...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.filterMode == FilterMode.ALL,
                            onClick = { viewModel.setFilterMode(FilterMode.ALL) },
                            label = { Text("All (${state.totalCount})") }
                        )
                        FilterChip(
                            selected = state.filterMode == FilterMode.VIDEO_ONLY,
                            onClick = { viewModel.setFilterMode(FilterMode.VIDEO_ONLY) },
                            label = { Text("Video (${state.videoCount})") }
                        )
                        FilterChip(
                            selected = state.filterMode == FilterMode.AUDIO_ONLY,
                            onClick = { viewModel.setFilterMode(FilterMode.AUDIO_ONLY) },
                            label = { Text("Audio (${state.audioCount})") }
                        )
                    }

                    // Storage summary
                    Text(
                        text = "Total storage used: ${formatFileSize(state.totalStorageBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        if (state.downloads.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    if (state.searchQuery.isNotEmpty()) "No matches found" else "No downloads yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(state.downloads, key = { it.id }) { download ->
                    DownloadGridItem(
                        download = download,
                        isSelected = download.id in state.selectedIds,
                        isSelectionMode = state.isSelectionMode,
                        onClick = {
                            if (state.isSelectionMode) viewModel.toggleSelection(download.id)
                            else navController.navigate(NavRoutes.playerRoute(download.id))
                        },
                        onLongClick = { viewModel.enterSelectionMode(download.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(state.downloads, key = { it.id }) { download ->
                    DownloadListItem(
                        download = download,
                        isSelected = download.id in state.selectedIds,
                        isSelectionMode = state.isSelectionMode,
                        onClick = {
                            if (state.isSelectionMode) viewModel.toggleSelection(download.id)
                            else navController.navigate(NavRoutes.playerRoute(download.id))
                        },
                        onLongClick = { viewModel.enterSelectionMode(download.id) }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DownloadGridItem(
    download: DownloadEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (download.isAudio) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(AmberPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(40.dp))
                    }
                } else {
                    AsyncImage(
                        model = download.thumbnailUrl,
                        contentDescription = download.title,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                }
                if (isSelectionMode) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = if (isSelected) AmberPrimary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    )
                }
            }
            Text(
                download.title,
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(8.dp)
            )
            Text(
                formatFileSize(download.fileSizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp).padding(bottom = 8.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DownloadListItem(
    download: DownloadEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(56.dp)) {
                if (download.isAudio) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(AmberPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AmberPrimary)
                    }
                } else {
                    AsyncImage(
                        model = download.thumbnailUrl,
                        contentDescription = download.title,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(download.title, maxLines = 1, fontWeight = FontWeight.Medium)
                Text(
                    "${download.siteName} • ${formatFileSize(download.fileSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelectionMode) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = if (isSelected) AmberPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

