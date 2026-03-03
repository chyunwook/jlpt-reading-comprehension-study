package com.example.jlpt_study.data.local

import androidx.room.TypeConverter
import com.example.jlpt_study.data.model.ErrorType
import com.example.jlpt_study.data.model.FunctionalBlock
import com.example.jlpt_study.data.model.WordStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room 데이터베이스용 타입 컨버터
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromFunctionalBlockList(value: List<FunctionalBlock>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFunctionalBlockList(value: String): List<FunctionalBlock> {
        if (value.isBlank() || value == "[]") return emptyList()
        val type = object : TypeToken<List<FunctionalBlock>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromErrorType(value: ErrorType): String {
        return value.name
    }

    @TypeConverter
    fun toErrorType(value: String): ErrorType {
        return ErrorType.valueOf(value)
    }

    @TypeConverter
    fun fromWordStatus(value: WordStatus): String {
        return value.name
    }

    @TypeConverter
    fun toWordStatus(value: String): WordStatus {
        return WordStatus.valueOf(value)
    }
}
