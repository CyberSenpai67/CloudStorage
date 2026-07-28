package com.example.data.model

data class BotInfo(
    val token: String = "",
    val chatId: String = "",
    val botName: String = "",
    val username: String = "",
    val isConnected: Boolean = false,
    val isDemoMode: Boolean = false
)

data class StorageStats(
    val totalSizeBytes: Long = 0,
    val imageSizeBytes: Long = 0,
    val videoSizeBytes: Long = 0,
    val documentSizeBytes: Long = 0,
    val audioSizeBytes: Long = 0,
    val archiveSizeBytes: Long = 0,
    val otherSizeBytes: Long = 0,
    val totalFileCount: Int = 0,
    val imageCount: Int = 0,
    val videoCount: Int = 0,
    val documentCount: Int = 0,
    val audioCount: Int = 0,
    val archiveCount: Int = 0,
    val otherCount: Int = 0
) {
    fun getPercentageForCategory(category: FileCategory): Float {
        if (totalSizeBytes <= 0) return 0f
        val bytes = when (category) {
            FileCategory.IMAGE -> imageSizeBytes
            FileCategory.VIDEO -> videoSizeBytes
            FileCategory.DOCUMENT -> documentSizeBytes
            FileCategory.AUDIO -> audioSizeBytes
            FileCategory.ARCHIVE -> archiveSizeBytes
            FileCategory.OTHER -> otherSizeBytes
            FileCategory.ALL -> totalSizeBytes
        }
        return (bytes.toFloat() / totalSizeBytes.toFloat()).coerceIn(0f, 1f)
    }
}
