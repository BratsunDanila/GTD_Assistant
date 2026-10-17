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
        Index("energyLevel"),
        Index("contextTag")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String? = null,     // Если часть "Большого дела"
    val orderInProject: Int = 0,       // Порядок шага в проекте (показываем только минимальный невыполненный)
    val originalQuote: String,         // Сырая фраза пользователя ("Из речи: «...»")
    val title: String,                 // Основной конкретный шаг
    val simplifiedSteps: List<String>, // 2 еще более простых варианта для кнопки "Проще"
    val currentSimplifyIndex: Int = 0, // 0 = title, 1 = simplifiedSteps[0], 2 = simplifiedSteps[1]
    val aiHint: String,                // Спокойный комментарий ИИ
    val categoryBadge: String,         // Короткий контекст ("Бытовое • Покупки", "Курсовая работа • Шаг 1/5")
    val estimatedMinutes: Int,         // Оценка времени в минутах (1, 2, 3, 5, 15, 20)
    val shelf: ShelfType,
    val energyLevel: EnergyLevel,
    val isCompleted: Boolean = false,
    val skippedCount: Int = 0,         // Сколько раз нажали "Потом"
    val lastShownAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val aiDraftType: String = "",       // "Черновик сообщения", "Стартовый план", "Поисковый фильтр ИИ"
    val aiDraftContent: String = "",    // Текст заготовки или 3 буллита от ИИ
    val scheduledDateMillis: Long? = null, // Время дня дедлайна в мс для Календаря
    val scheduledTimeText: String? = null, // "14:00", "18:00"
    val contextTag: String = "home"     // "pc" (Ноутбук), "phone" (Телефон), "home" (Дом), "out" (Покупки)
)
