package com.himanshu.liftly.core.data.db

import androidx.room.TypeConverter
import com.himanshu.liftly.core.domain.model.Muscle
import kotlinx.serialization.json.Json

internal class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun stringsToJson(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToStrings(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun musclesToJson(value: List<Muscle>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToMuscles(value: String): List<Muscle> = json.decodeFromString(value)
}
