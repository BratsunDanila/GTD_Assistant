package com.sbros.app.data.local

import androidx.room.TypeConverter
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
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
    fun fromEnergyLevel(value: EnergyLevel?): String = (value ?: EnergyLevel.MECHANICAL).name

    @TypeConverter
    fun toEnergyLevel(value: String?): EnergyLevel {
        if (value.isNullOrBlank()) return EnergyLevel.MECHANICAL
        return when (value.uppercase()) {
            "EASY", "MECHANICAL" -> EnergyLevel.MECHANICAL
            "FIVE_MIN", "SINGLE_STEP" -> EnergyLevel.SINGLE_STEP
            "FOCUS", "FLOW" -> EnergyLevel.FLOW
            else -> try {
                EnergyLevel.valueOf(value)
            } catch (e: Exception) {
                EnergyLevel.MECHANICAL
            }
        }
    }
}
