package com.waheed.universaldownloader.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.waheed.universaldownloader.data.settings.DefaultQuality
import com.waheed.universaldownloader.data.settings.ThemeMode
import com.waheed.universaldownloader.ui.components.GlassCard
import com.waheed.universaldownloader.ui.theme.AmberPrimary
import com.waheed.universaldownloader.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var folderNameInput by remember(state.downloadFolderName) { mutableStateOf(state.downloadFolderName) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)
        ) {
            // 1. Theme mode
            item {
                SettingRow(title = "Theme", subtitle = state.themeMode.name.lowercase().replaceFirstChar { it.uppercase() }) {
                    showThemeDialog = true
                }
            }
            // 2. Default download quality
            item {
                SettingRow(title = "Default quality", subtitle = state.defaultQuality.label) {
                    showQualityDialog = true
                }
            }
            // 3. Wi-Fi only downloads
            item {
                SettingToggleRow(
                    title = "Wi-Fi only downloads",
                    subtitle = "Avoid using mobile data",
                    icon = Icons.Filled.Wifi,
                    checked = state.wifiOnlyDownloads,
                    onCheckedChange = { viewModel.setWifiOnlyDownloads(it) }
                )
            }
            // 4. Notify on complete
            item {
                SettingToggleRow(
                    title = "Notify when download completes",
                    subtitle = null,
                    icon = Icons.Filled.Notifications,
                    checked = state.notifyOnComplete,
                    onCheckedChange = { viewModel.setNotifyOnComplete(it) }
                )
            }
            // 5. Auto-delete after share
            item {
                SettingToggleRow(
                    title = "Auto-delete after sharing",
                    subtitle = "Frees up space once you've shared a file",
                    icon = Icons.Filled.Delete,
                    checked = state.autoDeleteAfterShare,
                    onCheckedChange = { viewModel.setAutoDeleteAfterShare(it) }
                )
            }
            // 6. PIN lock
            item {
                SettingToggleRow(
                    title = "App PIN lock",
                    subtitle = "Require a PIN to open the app",
                    icon = Icons.Filled.Lock,
                    checked = state.isPinLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            navController.navigate("pinlock_setup")
                        } else {
                            viewModel.setPinLockEnabled(false)
                        }
                    }
                )
            }
            // 7. Custom download folder name
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Download folder name", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = folderNameInput,
                            onValueChange = { folderNameInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(onClick = { viewModel.setDownloadFolderName(folderNameInput) }) {
                                    Text("Save")
                                }
                            }
                        )
                    }
                }
            }
            // 8. Storage / cache info + clear cache
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cache size", style = MaterialTheme.typography.titleSmall)
                            Text(formatFileSize(state.cacheSizeBytes), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { showClearCacheConfirm = true }) {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }
            }
            // 9. Download count + clear all history
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Download history", style = MaterialTheme.typography.titleSmall)
                            Text("${state.downloadCount} items", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { showClearHistoryConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear all")
                        }
                    }
                }
            }
            // 10. Check for updates
            item {
                SettingRow(
                    title = "Check for updates",
                    subtitle = if (state.updateAvailable) "Update available!" else "You're up to date"
                ) {
                    viewModel.refreshCacheSize()
                }
            }
            // 11. App version
            item {
                SettingRow(title = "App version", subtitle = state.appVersion, icon = Icons.Filled.Info) {}
            }
            // 12. Share app
            item {
                SettingRow(title = "Share this app", icon = Icons.Filled.Share) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out Universal Downloader!")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                }
            }
            // 13. Rate app
            item {
                SettingRow(title = "Rate this app", icon = Icons.Filled.Star) {
                    // Placeholder — wire to actual Aptoide/Uptodown listing URL once published
                }
            }
            // 14. Privacy policy
            item {
                SettingRow(title = "Privacy policy") {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://waheed786dar-cell.github.io/privacy-policy/"))
                    context.startActivity(intent)
                }
            }
            // 15. About / build info
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("About", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Universal Downloader — built independently, powered by yt-dlp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose theme") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AmberPrimary)
                            )
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Default download quality") },
            text = {
                Column {
                    DefaultQuality.entries.forEach { quality ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.defaultQuality == quality,
                                onClick = {
                                    viewModel.setDefaultQuality(quality)
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AmberPrimary)
                            )
                            Text(quality.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Close") }
            }
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear cache?") },
            text = { Text("This will free up ${formatFileSize(state.cacheSizeBytes)} of temporary files.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear all download history?") },
            text = { Text("This removes all ${state.downloadCount} items from your library. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllDownloadHistory()
                    showClearHistoryConfirm = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = AmberPrimary)
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onClick) { Text("Change") }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AmberPrimary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = AmberPrimary)
            )
        }
    }
}

