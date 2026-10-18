package com.sbros.app.domain.model

enum class ShelfType {
    SIMPLE_STEPS, // Простые шаги (одиночные действия)
    BIG_THINGS,   // Большие дела (проекты из нескольких шагов)
    WAITING,      // Жду ответа (делегировано / ожидание)
    SOMEDAY,      // На потом и идеи (хотелки без сроков)
    REFERENCE     // Справочник и заметки (справочная информация и чеклисты)
}

enum class TaskDifficulty(val label: String) {
    EASY("Просто"),
    MEDIUM("Средне"),
    HARD("Сложно");

    companion object {
        val MECHANICAL get() = EASY
        val SINGLE_STEP get() = MEDIUM
        val FLOW get() = HARD
    }
}

typealias EnergyLevel = TaskDifficulty

enum class ThemePalette {
    LAVENDER, // Лаванда
    SAGE,     // Шалфей
    CLAY,     // Терракота
    OCEAN     // Океан
}

