package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CloudFile
import com.example.data.model.FileCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudFileDao {

    @Query("SELECT * FROM cloud_files ORDER BY uploadTimeMillis DESC")
    fun getAllFiles(): Flow<List<CloudFile>>

    @Query("SELECT * FROM cloud_files WHERE category = :category ORDER BY uploadTimeMillis DESC")
    fun getFilesByCategory(category: FileCategory): Flow<List<CloudFile>>

    @Query("SELECT * FROM cloud_files WHERE fileName LIKE '%' || :searchQuery || '%' ORDER BY uploadTimeMillis DESC")
    fun searchFiles(searchQuery: String): Flow<List<CloudFile>>

    @Query("SELECT * FROM cloud_files WHERE fileId = :fileId LIMIT 1")
    suspend fun getFileByTelegramFileId(fileId: String): CloudFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CloudFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<CloudFile>)

    @Delete
    suspend fun deleteFile(file: CloudFile)

    @Query("DELETE FROM cloud_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("DELETE FROM cloud_files WHERE isDemoFile = 1")
    suspend fun deleteDemoFiles()

    @Query("DELETE FROM cloud_files")
    suspend fun deleteAllFiles()

    @Query("SELECT SUM(sizeBytes) FROM cloud_files")
    fun getTotalStorageSize(): Flow<Long?>
}
