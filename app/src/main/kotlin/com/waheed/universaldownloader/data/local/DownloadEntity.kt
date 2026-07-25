package com.waheed.universaldownloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val filePath: String,
    val thumbnailUrl: String?,
    val siteName: String,
    val isAudio: Boolean,
    val fileSizeBytes: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)
