package com.waheed.universaldownloader.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert
    suspend fun insert(download: DownloadEntity): Long

    @Query("SELECT * FROM downloads ORDER BY createdAtMillis DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY createdAtMillis DESC LIMIT :limit")
    fun getRecentDownloads(limit: Int = 5): Flow<List<DownloadEntity>>

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)
}
