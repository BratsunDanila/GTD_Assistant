package com.sbros.app.data.local

import androidx.room.TypeConverter
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.SubstepItem
import com.sbros.app.domain.model.TaskDifficulty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSubstepList(value: List<SubstepItem>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toSubstepList(value: String?): List<SubstepItem> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<SubstepItem>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromShelfType(value: ShelfType?): String = (value ?: ShelfType.SIMPLE_STEPS).name

    @TypeConverter
    fun toShelfType(value: String?): ShelfType {
        if (value.isNullOrBlank()) return ShelfType.SIMPLE_STEPS
        return try {
            ShelfType.valueOf(value)
        } catch (e: Exception) {
            ShelfType.SIMPLE_STEPS
        }
    }

    @TypeConverter
    fun fromDifficulty(value: TaskDifficulty?): String = (value ?: TaskDifficulty.EASY).name

    @TypeConverter
    fun toDifficulty(value: String?): TaskDifficulty {
        if (value.isNullOrBlank()) return TaskDifficulty.EASY
        return when (value.uppercase()) {
            "EASY", "MECHANICAL" -> TaskDifficulty.EASY
            "MEDIUM", "FIVE_MIN", "SINGLE_STEP" -> TaskDifficulty.MEDIUM
            "HARD", "FOCUS", "FLOW" -> TaskDifficulty.HARD
            else -> try {
                TaskDifficulty.valueOf(value.uppercase())
            } catch (e: Exception) {
                TaskDifficulty.EASY
            }
        }
    }

    fun fromEnergyLevel(value: EnergyLevel?): String = fromDifficulty(value)

    fun toEnergyLevel(value: String?): EnergyLevel = toDifficulty(value)
}
