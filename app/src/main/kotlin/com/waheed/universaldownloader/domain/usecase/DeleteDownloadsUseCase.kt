package com.waheed.universaldownloader.domain.usecase

import com.waheed.universaldownloader.data.local.DownloadDao
import java.io.File
import javax.inject.Inject

/** Deletes one or more downloads: removes the DB rows AND the underlying files on disk. */
class DeleteDownloadsUseCase @Inject constructor(
    private val downloadDao: DownloadDao
) {
    suspend fun byIds(ids: List<Long>) {
        ids.forEach { id ->
            downloadDao.getById(id)?.let { entity ->
                runCatching { File(entity.filePath).delete() }
            }
        }
        downloadDao.deleteByIds(ids)
    }

    suspend fun all() {
        val all = downloadDao.getAllDownloadsOnce()
        all.forEach { entity -> runCatching { File(entity.filePath).delete() } }
        downloadDao.deleteAll()
    }
}
