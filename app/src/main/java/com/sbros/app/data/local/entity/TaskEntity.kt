package com.sbros.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.SubstepItem
import com.sbros.app.domain.model.TaskDifficulty
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
        Index("difficulty"),
        Index("contextCategory")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String? = null,     // Если часть "Большого дела"
    val orderInProject: Int = 0,       // Порядок шага в проекте (показываем только минимальный невыполненный)
    val originalQuote: String,         // Сырая фраза пользователя ("Из речи: «...»")
    val title: String,                 // Основной конкретный шаг
    val simplifiedSteps: List<String> = emptyList(), // Варианты упрощения
    val currentSimplifyIndex: Int = 0,
    val aiHint: String = "",           // Спокойный комментарий ИИ
    val categoryBadge: String = "",    // Короткий контекст
    val estimatedMinutes: Int = 5,     // Оценка времени в минутах
    val shelf: ShelfType = ShelfType.SIMPLE_STEPS,
    val difficulty: TaskDifficulty = TaskDifficulty.EASY,
    val isCompleted: Boolean = false,
    val skippedCount: Int = 0,         // Сколько раз нажали "Потом"
    val lastShownAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val aiDraftType: String = "",       // "Черновик сообщения", "Стартовый план"
    val aiDraftContent: String = "",    // Текст заготовки или 3 буллита от ИИ
    val scheduledDateMillis: Long? = null, // Время дня дедлайна в мс для Календаря
    val scheduledTimeText: String? = null, // "14:00", "18:00"
    val contextTag: String = "home",
    val contextCategory: String = "Дом и быт", // «Учеба и вуз», «Покупки и заказы», «Разработка и код», «Дом и быт»
    val substeps: List<SubstepItem> = emptyList(), // Чеклист шагов декомпозиции
    val aiSuggestedSteps: List<String> = emptyList(), // Шаги для кнопки "Разбить автоматически с помощью ИИ"
    val aiAdvice: String = ""           // Совет от ИИ
) {
    @Ignore
    val energyLevel: TaskDifficulty = difficulty
}

