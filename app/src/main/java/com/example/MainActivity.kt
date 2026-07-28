package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BotSetupScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FilePreviewDialog
import com.example.ui.screens.SettingsBottomSheet
import com.example.ui.theme.CloudHubTheme
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.CloudHubViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CloudHubTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CloudHubApp()
                }
            }
        }
    }
}

@Composable
fun CloudHubApp() {
    val context = LocalContext.current
    val viewModel: CloudHubViewModel = viewModel(
        factory = CloudHubViewModel.provideFactory(context)
    )

    val botInfo by viewModel.botInfo.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val files by viewModel.filteredFiles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val selectedFileForPreview by viewModel.selectedFileForPreview.collectAsStateWithLifecycle()
    val previewDownloadUrl by viewModel.previewDownloadUrl.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val connectState by viewModel.connectState.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }

    if (!botInfo.isConnected) {
        BotSetupScreen(
            connectState = connectState,
            onConnect = { token, chatId ->
                viewModel.validateAndConnectBot(token, chatId)
            },
            onAutoDetectChatId = { token ->
                viewModel.autoDetectChatId(token)
            },
            onStartDemo = {
                viewModel.startDemoMode()
            }
        )
    } else {
        DashboardScreen(
            botInfo = botInfo,
            storageStats = storageStats,
            files = files,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            isGridView = isGridView,
            uploadStatus = uploadStatus,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onCategorySelected = { viewModel.setSelectedCategory(it) },
            onToggleGridView = { viewModel.toggleGridView() },
            onSelectFile = { viewModel.selectFileForPreview(it) },
            onDeleteFile = { viewModel.deleteFile(it) },
            onUploadFilePicked = { uri ->
                viewModel.uploadSelectedFile(uri, context.contentResolver)
            },
            onOpenSettings = { showSettingsSheet = true }
        )

        selectedFileForPreview?.let { file ->
            FilePreviewDialog(
                file = file,
                downloadUrl = previewDownloadUrl,
                onDismiss = { viewModel.selectFileForPreview(null) },
                onDelete = {
                    viewModel.deleteFile(it)
                    viewModel.selectFileForPreview(null)
                }
            )
        }

        if (showSettingsSheet) {
            SettingsBottomSheet(
                botInfo = botInfo,
                totalFilesCount = storageStats.totalFileCount,
                totalStorageFormatted = FormatUtils.formatFileSize(storageStats.totalSizeBytes),
                onDisconnect = { viewModel.disconnectBot() },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}
