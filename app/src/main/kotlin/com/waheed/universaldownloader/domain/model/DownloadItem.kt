package com.waheed.universaldownloader.domain.model

import com.waheed.universaldownloader.data.local.DownloadEntity

/**
 * UI/domain-facing representation of a download, decoupled from the Room entity.
 * Screens and ViewModels should work with this, not DownloadEntity directly —
 * keeps persistence details out of the UI layer.
 */
data class DownloadItem(
    val id: Long,
    val title: String,
    val sourceUrl: String,
    val filePath: String,
    val thumbnailUrl: String?,
    val siteName: String,
    val isAudio: Boolean,
    val fileSizeBytes: Long,
    val createdAtMillis: Long
) {
    companion object {
        fun fromEntity(entity: DownloadEntity) = DownloadItem(
            id = entity.id,
            title = entity.title,
            sourceUrl = entity.sourceUrl,
            filePath = entity.filePath,
            thumbnailUrl = entity.thumbnailUrl,
            siteName = entity.siteName,
            isAudio = entity.isAudio,
            fileSizeBytes = entity.fileSizeBytes,
            createdAtMillis = entity.createdAtMillis
        )
    }
}

enum class MediaQuality(val formatSelector: String, val label: String) {
    BEST("best", "Best available"),
    HIGH_1080P("bestvideo[height<=1080]+bestaudio/best[height<=1080]", "1080p"),
    MEDIUM_720P("bestvideo[height<=720]+bestaudio/best[height<=720]", "720p"),
    LOW_480P("bestvideo[height<=480]+bestaudio/best[height<=480]", "480p"),
    AUDIO_ONLY("bestaudio", "Audio only")
}
