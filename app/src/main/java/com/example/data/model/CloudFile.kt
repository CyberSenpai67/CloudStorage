package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FileCategory(val displayName: String) {
    ALL("All Files"),
    IMAGE("Images"),
    VIDEO("Videos"),
    DOCUMENT("Documents"),
    AUDIO("Audio"),
    ARCHIVE("Archives"),
    OTHER("Others")
}

@Entity(tableName = "cloud_files")
data class CloudFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileId: String,
    val uniqueFileId: String = "",
    val messageId: Long = 0,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val category: FileCategory,
    val uploadTimeMillis: Long = System.currentTimeMillis(),
    val localUri: String? = null,
    val filePathOnTelegram: String? = null,
    val caption: String? = null,
    val isDemoFile: Boolean = false
)
