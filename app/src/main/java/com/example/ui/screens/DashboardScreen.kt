package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BotInfo
import com.example.data.model.CloudFile
import com.example.data.model.FileCategory
import com.example.data.model.StorageStats
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.UploadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    botInfo: BotInfo,
    storageStats: StorageStats,
    files: List<CloudFile>,
    searchQuery: String,
    selectedCategory: FileCategory,
    isGridView: Boolean,
    uploadStatus: UploadStatus?,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (FileCategory) -> Unit,
    onToggleGridView: () -> Unit,
    onSelectFile: (CloudFile) -> Unit,
    onDeleteFile: (CloudFile) -> Unit,
    onUploadFilePicked: (Uri) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadFilePicked(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cloudhub",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (botInfo.isDemoMode) "Demo Mode" else "@${botInfo.username.ifEmpty { "bot" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleGridView) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = "Toggle view style"
                        )
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("settings_button")) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("upload_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload File",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage Statistics Card ("How much storage we use")
            StorageStatsCard(
                stats = storageStats,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Active Upload Progress Banner
            if (uploadStatus != null) {
                UploadProgressBanner(
                    status = uploadStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Search Bar & Filter Chips
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search files by name...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_files_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(FileCategory.values()) { category ->
                        FilterChip(
                            selected = category == selectedCategory,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.displayName) },
                            leadingIcon = {
                                val icon = when (category) {
                                    FileCategory.ALL -> Icons.Default.LinearScale
                                    FileCategory.IMAGE -> Icons.Default.Image
                                    FileCategory.VIDEO -> Icons.Default.Movie
                                    FileCategory.DOCUMENT -> Icons.Default.Description
                                    FileCategory.AUDIO -> Icons.Default.AudioFile
                                    FileCategory.ARCHIVE -> Icons.Default.FolderZip
                                    FileCategory.OTHER -> Icons.Default.InsertDriveFile
                                }
                                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Files Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedCategory.displayName} (${files.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Empty Files View or Files List/Grid
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No files found for '$searchQuery'" else "No files in Telegram Cloud yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Upload File' below to store files securely in your Telegram Bot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(files, key = { it.id }) { file ->
                        FileGridItem(
                            file = file,
                            onClick = { onSelectFile(file) },
                            onDelete = { onDeleteFile(file) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(files, key = { it.id }) { file ->
                        FileListItem(
                            file = file,
                            onClick = { onSelectFile(file) },
                            onDelete = { onDeleteFile(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorageStatsCard(
    stats: StorageStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Telegram Cloud Storage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatFileSize(stats.totalSizeBytes),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "Unlimited Storage",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-segment category progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (stats.totalSizeBytes > 0) {
                        val imgWeight = stats.getPercentageForCategory(FileCategory.IMAGE)
                        val vidWeight = stats.getPercentageForCategory(FileCategory.VIDEO)
                        val docWeight = stats.getPercentageForCategory(FileCategory.DOCUMENT)
                        val audWeight = stats.getPercentageForCategory(FileCategory.AUDIO)
                        val archWeight = stats.getPercentageForCategory(FileCategory.ARCHIVE)
                        val othWeight = stats.getPercentageForCategory(FileCategory.OTHER)

                        if (imgWeight > 0) Box(modifier = Modifier.weight(imgWeight).fillMaxSize().background(Color(0xFF0284C7)))
                        if (vidWeight > 0) Box(modifier = Modifier.weight(vidWeight).fillMaxSize().background(Color(0xFF9333EA)))
                        if (docWeight > 0) Box(modifier = Modifier.weight(docWeight).fillMaxSize().background(Color(0xFFEA580C)))
                        if (audWeight > 0) Box(modifier = Modifier.weight(audWeight).fillMaxSize().background(Color(0xFF16A34A)))
                        if (archWeight > 0) Box(modifier = Modifier.weight(archWeight).fillMaxSize().background(Color(0xFFDB2777)))
                        if (othWeight > 0) Box(modifier = Modifier.weight(othWeight).fillMaxSize().background(Color(0xFF64748B)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage breakdown legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge(color = Color(0xFF0284C7), label = "Images", count = stats.imageCount)
                StatBadge(color = Color(0xFF9333EA), label = "Videos", count = stats.videoCount)
                StatBadge(color = Color(0xFFEA580C), label = "Docs", count = stats.documentCount)
                StatBadge(color = Color(0xFF16A34A), label = "Audio", count = stats.audioCount)
                StatBadge(color = Color(0xFFDB2777), label = "Archives", count = stats.archiveCount)
            }
        }
    }
}

@Composable
fun StatBadge(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UploadProgressBanner(
    status: UploadStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.fileName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = status.statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (status.isUploading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun FileListItem(
    file: CloudFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("file_item_${file.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (file.category) {
                    FileCategory.IMAGE -> Color(0xFFE0F2FE)
                    FileCategory.VIDEO -> Color(0xFFF3E8FF)
                    FileCategory.DOCUMENT -> Color(0xFFFFEDD5)
                    FileCategory.AUDIO -> Color(0xFFDCFCE7)
                    FileCategory.ARCHIVE -> Color(0xFFFCE7F3)
                    else -> Color(0xFFF1F5F9)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (file.category) {
                        FileCategory.IMAGE -> Icons.Default.Image
                        FileCategory.VIDEO -> Icons.Default.Movie
                        FileCategory.DOCUMENT -> Icons.Default.Description
                        FileCategory.AUDIO -> Icons.Default.AudioFile
                        FileCategory.ARCHIVE -> Icons.Default.FolderZip
                        else -> Icons.Default.InsertDriveFile
                    }
                    val iconTint = when (file.category) {
                        FileCategory.IMAGE -> Color(0xFF0284C7)
                        FileCategory.VIDEO -> Color(0xFF9333EA)
                        FileCategory.DOCUMENT -> Color(0xFFEA580C)
                        FileCategory.AUDIO -> Color(0xFF16A34A)
                        FileCategory.ARCHIVE -> Color(0xFFDB2777)
                        else -> Color(0xFF64748B)
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${FormatUtils.formatFileSize(file.sizeBytes)} • ${FormatUtils.formatDate(file.uploadTimeMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "File menu")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Preview & Details") },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete File", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileGridItem(
    file: CloudFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when (file.category) {
                    FileCategory.IMAGE -> Color(0xFFE0F2FE)
                    FileCategory.VIDEO -> Color(0xFFF3E8FF)
                    FileCategory.DOCUMENT -> Color(0xFFFFEDD5)
                    FileCategory.AUDIO -> Color(0xFFDCFCE7)
                    FileCategory.ARCHIVE -> Color(0xFFFCE7F3)
                    else -> Color(0xFFF1F5F9)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (file.category) {
                        FileCategory.IMAGE -> Icons.Default.Image
                        FileCategory.VIDEO -> Icons.Default.Movie
                        FileCategory.DOCUMENT -> Icons.Default.Description
                        FileCategory.AUDIO -> Icons.Default.AudioFile
                        FileCategory.ARCHIVE -> Icons.Default.FolderZip
                        else -> Icons.Default.InsertDriveFile
                    }
                    val iconTint = when (file.category) {
                        FileCategory.IMAGE -> Color(0xFF0284C7)
                        FileCategory.VIDEO -> Color(0xFF9333EA)
                        FileCategory.DOCUMENT -> Color(0xFFEA580C)
                        FileCategory.AUDIO -> Color(0xFF16A34A)
                        FileCategory.ARCHIVE -> Color(0xFFDB2777)
                        else -> Color(0xFF64748B)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = file.fileName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = FormatUtils.formatFileSize(file.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
