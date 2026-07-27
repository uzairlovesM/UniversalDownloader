package com.waheed.universaldownloader.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Drop this once near the app root (e.g. inside HomeScreen). Silently does nothing
 * when there's no update, and surfaces a blocking or dismissible dialog otherwise.
 */
@Composable
fun UpdatePromptDialog(viewModel: UpdateCheckViewModel = hiltViewModel()) {
    val state by viewModel.promptState.collectAsState()
    val context = LocalContext.current

    fun openUpdateUrl(url: String) {
        if (url.isNotBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    when (val promptState = state) {
        is UpdatePromptState.OptionalUpdate -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissOptionalUpdate() },
                title = { Text("Update available") },
                text = { Text("Version ${promptState.latestVersion} is ready with improvements and fixes.") },
                confirmButton = {
                    TextButton(onClick = { openUpdateUrl(promptState.apkUrl) }) { Text("Update now") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissOptionalUpdate() }) { Text("Later") }
                }
            )
        }
        is UpdatePromptState.ForcedUpdate -> {
            AlertDialog(
                onDismissRequest = { /* Not dismissible — this version is no longer supported */ },
                title = { Text("Update required") },
                text = { Text("This version is no longer supported. Please update to ${promptState.latestVersion} to continue.") },
                confirmButton = {
                    TextButton(onClick = { openUpdateUrl(promptState.apkUrl) }) { Text("Update now") }
                }
            )
        }
        is UpdatePromptState.MaintenanceMode -> {
            AlertDialog(
                onDismissRequest = { /* Not dismissible */ },
                title = { Text("Under maintenance") },
                text = { Text("Universal Downloader is temporarily unavailable. Please check back shortly.") },
                confirmButton = {}
            )
        }
        UpdatePromptState.None -> { /* no-op */ }
    }
}
