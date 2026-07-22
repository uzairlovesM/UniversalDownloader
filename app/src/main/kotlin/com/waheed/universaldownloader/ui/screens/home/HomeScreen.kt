package com.waheed.universaldownloader.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.waheed.universaldownloader.ui.components.GlassCard
import com.waheed.universaldownloader.ui.navigation.NavRoutes
import com.waheed.universaldownloader.ui.theme.AmberPrimary
import com.waheed.universaldownloader.ui.theme.TextSecondary

private data class SupportedSite(val name: String, val emoji: String)

private val supportedSites = listOf(
    SupportedSite("YouTube", "▶️"),
    SupportedSite("Instagram", "📸"),
    SupportedSite("TikTok", "🎵"),
    SupportedSite("Facebook", "👤"),
    SupportedSite("Pinterest", "📌"),
    SupportedSite("Twitter/X", "🐦")
)

private data class RecentDownload(val title: String, val site: String, val isAudio: Boolean)

// Placeholder sample data — will come from Room DB once the download engine is wired
private val sampleRecents = listOf(
    RecentDownload("Sample video title goes here", "YouTube", false),
    RecentDownload("Another downloaded clip", "Instagram", false),
    RecentDownload("Extracted audio track", "TikTok", true)
)

@Composable
fun HomeScreen(navController: NavHostController) {
    val clipboardManager = LocalClipboardManager.current
    var linkText by remember { mutableStateOf("") }
    var showClipboardHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val clipped = clipboardManager.getText()?.text.orEmpty()
        if (clipped.startsWith("http")) {
            showClipboardHint = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Universal Downloader",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(NavRoutes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Link input card ──
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Paste a link to get started",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = AmberPrimary) },
                        trailingIcon = {
                            IconButton(onClick = {
                                linkText = clipboardManager.getText()?.text.orEmpty()
                            }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", tint = TextSecondary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                        )
                    )

                    AnimatedVisibility(visible = showClipboardHint && linkText.isEmpty()) {
                        TextButton(
                            onClick = {
                                linkText = clipboardManager.getText()?.text.orEmpty()
                                showClipboardHint = false
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Link detected in clipboard — tap to paste", color = AmberPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (linkText.isNotBlank()) {
                                navController.navigate(NavRoutes.previewRoute(linkText))
                            }
                        },
                        enabled = linkText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = Color.Black,
                            disabledContainerColor = AmberPrimary.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetch", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Supported sites row ──
            Text(
                "Supported sites",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(supportedSites) { site ->
                    SiteChip(site)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Recent downloads section ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent downloads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { navController.navigate(NavRoutes.LIBRARY) }) {
                    Text("See all", color = AmberPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sampleRecents.isEmpty()) {
                EmptyState()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sampleRecents.forEach { item ->
                        RecentDownloadRow(item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SiteChip(site: SupportedSite) {
    GlassCard(cornerRadius = 20) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(site.emoji, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(6.dp))
            Text(site.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun RecentDownloadRow(item: RecentDownload) {
    val scale by animateFloatAsState(targetValue = 1f, animationSpec = tween(300), label = "rowScale")

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* navigate to player, wired once engine exists */ }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(AmberPrimary, AmberPrimary.copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isAudio) Icons.Filled.LibraryMusic else Icons.Filled.Download,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(item.site, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No downloads yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
