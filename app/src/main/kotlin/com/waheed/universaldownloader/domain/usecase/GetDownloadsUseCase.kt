package com.waheed.universaldownloader.domain.usecase

import com.waheed.universaldownloader.data.local.DownloadDao
import com.waheed.universaldownloader.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Fetches all downloads as domain models, ordered newest-first. */
class GetDownloadsUseCase @Inject constructor(
    private val downloadDao: DownloadDao
) {
    operator fun invoke(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { list -> list.map { DownloadItem.fromEntity(it) } }
    }
}
