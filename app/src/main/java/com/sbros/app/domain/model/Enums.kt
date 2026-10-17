package com.sbros.app.domain.model

enum class ShelfType {
    SIMPLE_STEPS, // Простые шаги (одиночные действия)
    BIG_THINGS,   // Большие дела (проекты из нескольких шагов)
    WAITING,      // Жду ответа (делегировано / ожидание)
    SOMEDAY,      // На потом и идеи (хотелки без сроков)
    REFERENCE     // Справочник и заметки (справочная информация и чеклисты)
}

enum class EnergyLevel {
    MECHANICAL,  // Механика (рутина на автомате, иконка Bolt)
    SINGLE_STEP, // Один шаг (понятное дело на 10-15 мин, иконка AdsClick)
    FLOW         // Поток (глубокая концентрация, иконка Psychology)
}

enum class ThemePalette {
    LAVENDER, // Лаванда
    SAGE,     // Шалфей
    CLAY,     // Терракота
    OCEAN     // Океан
}
