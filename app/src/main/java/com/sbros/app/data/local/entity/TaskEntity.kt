package com.sbros.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import java.util.UUID

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
        Index("shelf"),
        Index("energyLevel")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String? = null,     // Если часть "Большого дела"
    val orderInProject: Int = 0,       // Порядок шага в проекте (показываем только минимальный невыполненный)
    val originalQuote: String,         // Сырая фраза пользователя ("надо сесть за курсач")
    val title: String,                 // Основной конкретный шаг ("Написать черновик введения (3 абзаца)")
    val simplifiedSteps: List<String>, // 2 еще более простых варианта для кнопки "Проще"
    val currentSimplifyIndex: Int = 0, // 0 = title, 1 = simplifiedSteps[0], 2 = simplifiedSteps[1]
    val aiHint: String,                // Спокойный комментарий ИИ ("ИИ спрятал остальной курсач, чтобы он не давил")
    val categoryBadge: String,         // Короткий контекст ("Бытовое • Не требует фокуса")
    val estimatedMinutes: Int,         // Оценка времени в минутах (1, 3, 5, 20)
    val shelf: ShelfType,
    val energyLevel: EnergyLevel,
    val isCompleted: Boolean = false,
    val skippedCount: Int = 0,         // Сколько раз нажали "Не сейчас" (для мягкой амнистии)
    val lastShownAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
