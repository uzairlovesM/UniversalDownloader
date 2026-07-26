package com.waheed.universaldownloader.ui.screens.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.waheed.universaldownloader.ui.components.GlassCard
import com.waheed.universaldownloader.ui.navigation.NavRoutes
import com.waheed.universaldownloader.ui.theme.AmberPrimary
import com.waheed.universaldownloader.ui.theme.ErrorRed
import com.waheed.universaldownloader.ui.theme.SuccessGreen
import com.waheed.universaldownloader.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    url: String,
    title: String,
    thumbnailUrl: String?,
    siteName: String,
    isAudioOnly: Boolean,
    formatSelector: String,
    navController: NavHostController,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(url) {
        viewModel.startDownload(url, title, thumbnailUrl, siteName, isAudioOnly, formatSelector)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Downloading", color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val current = state) {
                is DownloadProgressState.InProgress -> InProgressContent(current, title)
                is DownloadProgressState.Completed -> CompletedContent(title) {
                    navController.navigate(NavRoutes.LIBRARY) {
                        popUpTo(NavRoutes.HOME)
                    }
                }
                is DownloadProgressState.Failed -> FailedContent(current.message) {
                    navController.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun InProgressContent(state: DownloadProgressState.InProgress, title: String) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.percent / 100f,
        animationSpec = tween(300),
        label = "downloadProgress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(120.dp),
                color = AmberPrimary,
                strokeWidth = 8.dp,
                trackColor = TextSecondary.copy(alpha = 0.15f)
            )
            Text(
                "${state.percent.toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (state.etaSeconds > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("About ${state.etaSeconds}s remaining", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CompletedContent(title: String, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Download complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 2)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("View in Library", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FailedContent(message: String, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(ErrorRed.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Download failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(6.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Go Back", fontWeight = FontWeight.SemiBold)
        }
    }
}
