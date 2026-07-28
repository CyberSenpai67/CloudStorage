package com.example.ui.util

import com.example.data.model.FileCategory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun formatDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    fun getCategoryIcon(category: FileCategory): Int {
        return when (category) {
            FileCategory.IMAGE -> android.R.drawable.ic_menu_gallery
            FileCategory.VIDEO -> android.R.drawable.ic_media_play
            FileCategory.DOCUMENT -> android.R.drawable.ic_menu_agenda
            FileCategory.AUDIO -> android.R.drawable.ic_media_play
            FileCategory.ARCHIVE -> android.R.drawable.ic_menu_save
            FileCategory.OTHER -> android.R.drawable.ic_menu_info_details
            FileCategory.ALL -> android.R.drawable.ic_menu_search
        }
    }
}
