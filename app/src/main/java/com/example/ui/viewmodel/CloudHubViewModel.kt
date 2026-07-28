package com.example.ui.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.BotInfo
import com.example.data.model.CloudFile
import com.example.data.model.FileCategory
import com.example.data.model.StorageStats
import com.example.data.repository.CloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UploadStatus(
    val fileName: String,
    val progress: Float,
    val statusText: String,
    val isUploading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class BotConnectState(
    val isLoading: Boolean = false,
    val botName: String? = null,
    val botUsername: String? = null,
    val detectedChatId: String? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class CloudHubViewModel(
    private val repository: CloudRepository
) : ViewModel() {

    val botInfo: StateFlow<BotInfo> = repository.botInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BotInfo()
    )

    val storageStats: StateFlow<StorageStats> = repository.storageStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StorageStats()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FileCategory.ALL)
    val selectedCategory: StateFlow<FileCategory> = _selectedCategory.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedFileForPreview = MutableStateFlow<CloudFile?>(null)
    val selectedFileForPreview: StateFlow<CloudFile?> = _selectedFileForPreview.asStateFlow()

    private val _previewDownloadUrl = MutableStateFlow<String?>(null)
    val previewDownloadUrl: StateFlow<String?> = _previewDownloadUrl.asStateFlow()

    private val _uploadStatus = MutableStateFlow<UploadStatus?>(null)
    val uploadStatus: StateFlow<UploadStatus?> = _uploadStatus.asStateFlow()

    private val _connectState = MutableStateFlow(BotConnectState())
    val connectState: StateFlow<BotConnectState> = _connectState.asStateFlow()

    val filteredFiles: StateFlow<List<CloudFile>> = combine(
        repository.allFiles,
        _searchQuery,
        _selectedCategory
    ) { files, query, category ->
        files.filter { file ->
            val matchesCategory = (category == FileCategory.ALL) || (file.category == category)
            val matchesQuery = query.isBlank() || file.fileName.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedDemoDataIfEmpty()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: FileCategory) {
        _selectedCategory.value = category
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun selectFileForPreview(file: CloudFile?) {
        _selectedFileForPreview.value = file
        _previewDownloadUrl.value = null

        if (file != null) {
            viewModelScope.launch {
                val url = repository.getDownloadUrl(file)
                _previewDownloadUrl.value = url
            }
        }
    }

    fun validateAndConnectBot(token: String, chatIdInput: String) {
        val cleanToken = token.trim()
        var cleanChatId = chatIdInput.trim()

        if (cleanToken.isEmpty()) {
            _connectState.value = BotConnectState(error = "Please enter your Telegram Bot Token")
            return
        }

        viewModelScope.launch {
            _connectState.value = BotConnectState(isLoading = true)

            val botResult = repository.validateBotToken(cleanToken)
            if (!botResult.isValid) {
                _connectState.value = BotConnectState(
                    error = botResult.errorMessage ?: "Invalid Bot Token"
                )
                return@launch
            }

            // Auto-detect chat ID if missing
            if (cleanChatId.isEmpty()) {
                val autoChatId = repository.detectChatId(cleanToken)
                if (autoChatId != null) {
                    cleanChatId = autoChatId
                } else {
                    _connectState.value = BotConnectState(
                        botName = botResult.botName,
                        botUsername = botResult.username,
                        error = "Bot valid! Please enter Chat ID or send a message on Telegram to @${botResult.username} so we can detect your Chat ID."
                    )
                    return@launch
                }
            }

            repository.saveBotCredentials(
                token = cleanToken,
                chatId = cleanChatId,
                botName = botResult.botName,
                username = botResult.username
            )

            _connectState.value = BotConnectState(
                isSuccess = true,
                botName = botResult.botName,
                botUsername = botResult.username,
                detectedChatId = cleanChatId
            )
        }
    }

    fun autoDetectChatId(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isEmpty()) return

        viewModelScope.launch {
            _connectState.value = _connectState.value.copy(isLoading = true, error = null)
            val chatId = repository.detectChatId(cleanToken)
            if (chatId != null) {
                _connectState.value = _connectState.value.copy(
                    isLoading = false,
                    detectedChatId = chatId,
                    error = null
                )
            } else {
                _connectState.value = _connectState.value.copy(
                    isLoading = false,
                    error = "No recent messages found. Send any message or /start to your bot on Telegram, then try again."
                )
            }
        }
    }

    fun startDemoMode() {
        repository.enableDemoMode()
    }

    fun disconnectBot() {
        repository.disconnectBot()
        _connectState.value = BotConnectState()
    }

    fun uploadSelectedFile(uri: Uri, contentResolver: ContentResolver) {
        val (fileName, mimeType) = getFileInfo(uri, contentResolver)

        _uploadStatus.value = UploadStatus(
            fileName = fileName,
            progress = 0f,
            statusText = "Preparing upload...",
            isUploading = true
        )

        viewModelScope.launch {
            val result = repository.uploadFile(
                fileUri = uri,
                fileName = fileName,
                mimeType = mimeType,
                contentResolver = contentResolver,
                onProgress = { progress ->
                    _uploadStatus.value = UploadStatus(
                        fileName = fileName,
                        progress = progress,
                        statusText = "Uploading ${(progress * 100).toInt()}%...",
                        isUploading = true
                    )
                }
            )

            result.onSuccess { cloudFile ->
                _uploadStatus.value = UploadStatus(
                    fileName = fileName,
                    progress = 1.0f,
                    statusText = "Successfully uploaded to Telegram Cloud!",
                    isUploading = false,
                    isSuccess = true
                )
            }.onFailure { exception ->
                _uploadStatus.value = UploadStatus(
                    fileName = fileName,
                    progress = 0f,
                    statusText = "Upload failed: ${exception.localizedMessage}",
                    isUploading = false,
                    isSuccess = false,
                    errorMessage = exception.localizedMessage
                )
            }
        }
    }

    fun dismissUploadStatus() {
        _uploadStatus.value = null
    }

    fun deleteFile(file: CloudFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
            if (_selectedFileForPreview.value?.id == file.id) {
                selectFileForPreview(null)
            }
        }
    }

    private fun getFileInfo(uri: Uri, contentResolver: ContentResolver): Pair<String, String> {
        var name = "file_${System.currentTimeMillis()}"
        var type = contentResolver.getType(uri) ?: "application/octet-stream"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        name = displayName
                    }
                }
            }
        }
        return Pair(name, type)
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(context)
                val userPrefs = UserPreferencesRepository(context)
                val repository = CloudRepository(db.cloudFileDao(), userPrefs)
                return CloudHubViewModel(repository) as T
            }
        }
    }
}
