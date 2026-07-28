package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FileCategory

class Converters {
    @TypeConverter
    fun fromFileCategory(category: FileCategory): String {
        return category.name
    }

    @TypeConverter
    fun toFileCategory(value: String): FileCategory {
        return try {
            FileCategory.valueOf(value)
        } catch (e: Exception) {
            FileCategory.OTHER
        }
    }
}
