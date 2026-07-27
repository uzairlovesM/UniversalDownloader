package com.waheed.universaldownloader.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert
    suspend fun insert(download: DownloadEntity): Long

    @Query("SELECT * FROM downloads ORDER BY createdAtMillis DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads ORDER BY createdAtMillis DESC LIMIT :limit")
    fun getRecentDownloads(limit: Int = 5): Flow<List<DownloadEntity>>

    // Library screen: search by title (case-insensitive substring match)
    @Query("""
        SELECT * FROM downloads
        WHERE (:searchQuery = '' OR title LIKE '%' || :searchQuery || '%')
        AND (:audioOnly = 0 OR isAudio = 1)
        AND (:videoOnly = 0 OR isAudio = 0)
        ORDER BY
            CASE WHEN :sortBy = 'name_asc' THEN title END ASC,
            CASE WHEN :sortBy = 'name_desc' THEN title END DESC,
            CASE WHEN :sortBy = 'size_asc' THEN fileSizeBytes END ASC,
            CASE WHEN :sortBy = 'size_desc' THEN fileSizeBytes END DESC,
            CASE WHEN :sortBy = 'oldest' THEN createdAtMillis END ASC,
            createdAtMillis DESC
    """)
    fun searchAndFilter(
        searchQuery: String = "",
        audioOnly: Boolean = false,
        videoOnly: Boolean = false,
        sortBy: String = "newest"
    ): Flow<List<DownloadEntity>>

    @Query("SELECT COUNT(*) FROM downloads")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM downloads")
    fun getTotalStorageBytes(): Flow<Long>

    @Query("SELECT COUNT(*) FROM downloads WHERE isAudio = 1")
    fun getAudioCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloads WHERE isAudio = 0")
    fun getVideoCount(): Flow<Int>

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Multi-select batch delete
    @Query("DELETE FROM downloads WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
