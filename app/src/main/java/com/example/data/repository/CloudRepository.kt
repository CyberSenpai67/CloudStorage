package com.example.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.data.local.CloudFileDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.BotInfo
import com.example.data.model.CloudFile
import com.example.data.model.FileCategory
import com.example.data.model.StorageStats
import com.example.data.remote.TelegramApiService
import com.example.data.remote.TelegramBotResult
import com.example.data.remote.TelegramUploadResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CloudRepository(
    private val dao: CloudFileDao,
    private val prefsRepository: UserPreferencesRepository,
    private val apiService: TelegramApiService = TelegramApiService()
) {

    val botInfo: Flow<BotInfo> = prefsRepository.botInfo

    val allFiles: Flow<List<CloudFile>> = dao.getAllFiles()

    val storageStats: Flow<StorageStats> = dao.getAllFiles().map { files ->
        var totalSize = 0L
        var imgSize = 0L
        var vidSize = 0L
        var docSize = 0L
        var audSize = 0L
        var archSize = 0L
        var othSize = 0L

        var imgCount = 0
        var vidCount = 0
        var docCount = 0
        var audCount = 0
        var archCount = 0
        var othCount = 0

        for (file in files) {
            totalSize += file.sizeBytes
            when (file.category) {
                FileCategory.IMAGE -> {
                    imgSize += file.sizeBytes
                    imgCount++
                }
                FileCategory.VIDEO -> {
                    vidSize += file.sizeBytes
                    vidCount++
                }
                FileCategory.DOCUMENT -> {
                    docSize += file.sizeBytes
                    docCount++
                }
                FileCategory.AUDIO -> {
                    audSize += file.sizeBytes
                    audCount++
                }
                FileCategory.ARCHIVE -> {
                    archSize += file.sizeBytes
                    archCount++
                }
                FileCategory.OTHER -> {
                    othSize += file.sizeBytes
                    othCount++
                }
                FileCategory.ALL -> {}
            }
        }

        StorageStats(
            totalSizeBytes = totalSize,
            imageSizeBytes = imgSize,
            videoSizeBytes = vidSize,
            documentSizeBytes = docSize,
            audioSizeBytes = audSize,
            archiveSizeBytes = archSize,
            otherSizeBytes = othSize,
            totalFileCount = files.size,
            imageCount = imgCount,
            videoCount = vidCount,
            documentCount = docCount,
            audioCount = audCount,
            archiveCount = archCount,
            otherCount = othCount
        )
    }

    suspend fun validateBotToken(token: String): TelegramBotResult {
        return apiService.validateBotToken(token)
    }

    suspend fun detectChatId(token: String): String? {
        return apiService.fetchLatestChatId(token)
    }

    fun saveBotCredentials(token: String, chatId: String, botName: String, username: String) {
        prefsRepository.saveBotCredentials(token, chatId, botName, username)
    }

    fun enableDemoMode() {
        prefsRepository.enableDemoMode()
    }

    fun disconnectBot() {
        prefsRepository.clearCredentials()
    }

    suspend fun uploadFile(
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        contentResolver: ContentResolver,
        onProgress: (Float) -> Unit
    ): Result<CloudFile> {
        val currentBot = prefsRepository.botInfo.value

        val category = determineCategory(fileName, mimeType)

        if (currentBot.isDemoMode || !currentBot.isConnected) {
            // Simulated Upload in Demo Mode
            val simulatedSize = (500_000L..15_000_000L).random()
            val simulatedFile = CloudFile(
                fileId = "demo_file_${System.currentTimeMillis()}",
                uniqueFileId = "demo_uniq_${System.currentTimeMillis()}",
                messageId = (1000..9999).random().toLong(),
                fileName = fileName,
                mimeType = mimeType.ifEmpty { "application/octet-stream" },
                sizeBytes = simulatedSize,
                category = category,
                uploadTimeMillis = System.currentTimeMillis(),
                localUri = fileUri.toString(),
                caption = "Uploaded to Cloudhub Demo Bot",
                isDemoFile = true
            )
            // Progress simulation
            for (p in 1..10) {
                onProgress(p / 10f)
                kotlinx.coroutines.delay(100)
            }
            dao.insertFile(simulatedFile)
            return Result.success(simulatedFile)
        }

        val uploadResult = apiService.uploadFile(
            token = currentBot.token,
            chatId = currentBot.chatId,
            fileUri = fileUri,
            fileName = fileName,
            mimeType = mimeType,
            category = category,
            contentResolver = contentResolver,
            onProgress = onProgress
        )

        if (!uploadResult.success) {
            return Result.failure(Exception(uploadResult.errorMessage ?: "Upload failed"))
        }

        val cloudFile = CloudFile(
            fileId = uploadResult.fileId,
            uniqueFileId = uploadResult.uniqueFileId,
            messageId = uploadResult.messageId,
            fileName = uploadResult.fileName,
            mimeType = uploadResult.mimeType,
            sizeBytes = uploadResult.sizeBytes,
            category = category,
            uploadTimeMillis = System.currentTimeMillis(),
            localUri = fileUri.toString(),
            filePathOnTelegram = uploadResult.filePathOnTelegram,
            caption = "Uploaded via Cloudhub Bot",
            isDemoFile = false
        )

        val rowId = dao.insertFile(cloudFile)
        return Result.success(cloudFile.copy(id = rowId))
    }

    suspend fun deleteFile(file: CloudFile): Boolean {
        val currentBot = prefsRepository.botInfo.value
        if (currentBot.isConnected && !currentBot.isDemoMode && file.messageId > 0) {
            apiService.deleteMessage(currentBot.token, currentBot.chatId, file.messageId)
        }
        dao.deleteFile(file)
        return true
    }

    suspend fun getDownloadUrl(file: CloudFile): String? {
        val currentBot = prefsRepository.botInfo.value
        if (file.filePathOnTelegram != null && currentBot.token.isNotEmpty()) {
            return apiService.buildDirectDownloadUrl(currentBot.token, file.filePathOnTelegram)
        }
        if (file.fileId.isNotEmpty() && currentBot.token.isNotEmpty() && !currentBot.isDemoMode) {
            val path = apiService.getTelegramFilePath(currentBot.token, file.fileId)
            if (path != null) {
                return apiService.buildDirectDownloadUrl(currentBot.token, path)
            }
        }
        return file.localUri
    }

    suspend fun seedDemoDataIfEmpty() {
        val existing = dao.getFileByTelegramFileId("demo_seed_1")
        if (existing == null) {
            val demoList = listOf(
                CloudFile(
                    fileId = "demo_seed_1",
                    fileName = "Project_Presentation_2026.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = 4_520_000L,
                    category = FileCategory.DOCUMENT,
                    uploadTimeMillis = System.currentTimeMillis() - 86400000L * 2,
                    isDemoFile = true
                ),
                CloudFile(
                    fileId = "demo_seed_2",
                    fileName = "Cloudhub_Banner_Hero.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 2_150_000L,
                    category = FileCategory.IMAGE,
                    uploadTimeMillis = System.currentTimeMillis() - 86400000L * 1,
                    isDemoFile = true
                ),
                CloudFile(
                    fileId = "demo_seed_3",
                    fileName = "App_Demo_Walkthrough.mp4",
                    mimeType = "video/mp4",
                    sizeBytes = 28_400_000L,
                    category = FileCategory.VIDEO,
                    uploadTimeMillis = System.currentTimeMillis() - 3600000L * 5,
                    isDemoFile = true
                ),
                CloudFile(
                    fileId = "demo_seed_4",
                    fileName = "Podcast_Episode_01.mp3",
                    mimeType = "audio/mpeg",
                    sizeBytes = 12_800_000L,
                    category = FileCategory.AUDIO,
                    uploadTimeMillis = System.currentTimeMillis() - 3600000L * 2,
                    isDemoFile = true
                ),
                CloudFile(
                    fileId = "demo_seed_5",
                    fileName = "Database_Backup.zip",
                    mimeType = "application/zip",
                    sizeBytes = 18_900_000L,
                    category = FileCategory.ARCHIVE,
                    uploadTimeMillis = System.currentTimeMillis() - 1800000L,
                    isDemoFile = true
                )
            )
            dao.insertFiles(demoList)
        }
    }

    private fun determineCategory(fileName: String, mimeType: String): FileCategory {
        val lowerName = fileName.lowercase()
        val lowerMime = mimeType.lowercase()

        return when {
            lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") -> FileCategory.IMAGE
            lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".avi") || lowerName.endsWith(".webm") || lowerName.endsWith(".mov") -> FileCategory.VIDEO
            lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".ogg") || lowerName.endsWith(".m4a") || lowerName.endsWith(".flac") -> FileCategory.AUDIO
            lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".7z") || lowerName.endsWith(".tar") || lowerName.endsWith(".gz") -> FileCategory.ARCHIVE
            lowerMime.contains("pdf") || lowerMime.contains("text") || lowerMime.contains("document") || lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || lowerName.endsWith(".txt") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".pptx") -> FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }
}
