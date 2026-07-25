package com.waheed.universaldownloader.ui.screens.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.waheed.universaldownloader.ui.components.GlassCard
import com.waheed.universaldownloader.ui.components.ShimmerBox
import com.waheed.universaldownloader.ui.navigation.NavRoutes
import com.waheed.universaldownloader.ui.theme.AmberPrimary
import com.waheed.universaldownloader.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    url: String,
    navController: NavHostController,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(url) {
        viewModel.fetchInfo(url)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Preview", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is PreviewUiState.Loading -> LoadingContent()
                is PreviewUiState.Error -> ErrorContent(state.message) { viewModel.fetchInfo(url) }
                is PreviewUiState.Success -> VideoInfoContent(state, navController, url)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ShimmerBox(modifier = Modifier.size(200.dp), cornerRadius = 20)
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(color = AmberPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Fetching video info…", color = TextSecondary)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Couldn't fetch this link", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black)) {
            Text("Retry")
        }
    }
}

@Composable
private fun VideoInfoContent(
    state: PreviewUiState.Success,
    navController: NavHostController,
    url: String
) {
    val info = state.info
    var selectedFormat by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
            Column {
                AsyncImage(
                    model = info.thumbnail,
                    contentDescription = info.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        info.title ?: "Untitled",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2
                    )
                    info.duration?.let { duration ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${duration / 60}:${(duration % 60).toString().padStart(2, '0')}", color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Choose quality", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf("1080p", "720p", "480p", "360p")) { quality ->
                QualityOption(
                    label = quality,
                    isSelected = selectedFormat == quality,
                    onClick = { selectedFormat = quality }
                )
            }
            item {
                QualityOption(
                    label = "Audio only (MP3)",
                    icon = Icons.Filled.MusicNote,
                    isSelected = selectedFormat == "audio",
                    onClick = { selectedFormat = "audio" }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val chosenFormat = selectedFormat ?: return@Button
                val isAudio = chosenFormat == "audio"
                val formatSelector = when (chosenFormat) {
                    "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]"
                    "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]"
                    "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]"
                    "360p" -> "bestvideo[height<=360]+bestaudio/best[height<=360]"
                    else -> "bestaudio"
                }
                val siteName = try {
                    java.net.URI(url).host?.removePrefix("www.") ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }

                navController.navigate(
                    NavRoutes.progressRoute(
                        url = url,
                        title = info.title ?: "Untitled",
                        thumbnail = info.thumbnail,
                        site = siteName,
                        isAudio = isAudio,
                        format = formatSelector
                    )
                )
            },
            enabled = selectedFormat != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black)
        ) {
            Text("Download", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QualityOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        cornerRadius = 14
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = AmberPrimary)
            )
            icon?.let {
                Icon(it, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(label, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
