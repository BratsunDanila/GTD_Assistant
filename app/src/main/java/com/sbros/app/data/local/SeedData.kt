package com.sbros.app.data.local

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import java.util.Calendar

object SeedData {

    private fun getCalTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getRelativeTimestamp(offsetDays: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val initialProjects = listOf(
        ProjectEntity(
            id = "project-coursework",
            title = "Курсовая работа",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        ProjectEntity(
            id = "project-sbros",
            title = "Приложение Сброс",
            createdAt = System.currentTimeMillis() - 86400000L
        )
    )

    val initialTasks = listOf(
        // 1. Механика (MECHANICAL) — Заказ батареек
        TaskEntity(
            id = "task-mech-batteries-buy",
            projectId = null,
            orderInProject = 0,
            originalQuote = "Из речи: «...не забыть купить аккумуляторы АА для камеры...»",
            title = "Заказать аккумуляторы АА на маркетплейсе",
            simplifiedSteps = listOf(
                "Открыть маркетплейс и вбить «Аккумуляторы АА Ni-MH 4 шт»",
                "Положить первую подходящую упаковку в корзину"
            ),
            currentSimplifyIndex = 0,
            aiHint = "ИИ отделил это от мыслей про камеру в простую покупку.",
            categoryBadge = "Бытовое • Покупки",
            estimatedMinutes = 3,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.MECHANICAL,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 1000L,
            createdAt = System.currentTimeMillis() - 3600000L * 3,
            aiDraftType = "Рекомендация ИИ",
            aiDraftContent = "Поиск: Ni-MH AA 2100-2500 mAh (низкий саморазряд LSD, держат привод объектива без просадки напряжения).",
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 17, 19, 30),
            scheduledTimeText = "19:30",
            contextTag = "out"
        ),

        // 2. Механика (MECHANICAL) — Зарядка батареек
        TaskEntity(
            id = "task-mech-batteries-charge",
            projectId = null,
            orderInProject = 0,
            originalQuote = "Из речи: «...поставить старые батарейки заряжаться...»",
            title = "Поставить старые аккумуляторы в зарядник",
            simplifiedSteps = listOf(
                "Достать зарядное устройство из ящика стола",
                "Положить аккумуляторы рядом с розеткой"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Легкое физическое действие, чтобы переключиться.",
            categoryBadge = "Дом • Механика",
            estimatedMinutes = 2,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.MECHANICAL,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 2000L,
            createdAt = System.currentTimeMillis() - 3600000L * 2,
            aiDraftType = "Совет ИИ",
            aiDraftContent = "Совет: проверь, чтобы оба аккумулятора были из одной серии перед зарядкой.",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "home"
        ),

        // 3. Механика (MECHANICAL) — Быстрый хвост до 2 минут (Quick Win)
        TaskEntity(
            id = "task-quick-win-telegram",
            projectId = null,
            orderInProject = 0,
            originalQuote = "Из речи: «...старосте надо тему работы скинуть...»",
            title = "Отправить старосте тему работы в ТГ",
            simplifiedSteps = listOf(
                "Открыть диалог со старостой",
                "Вставить готовую заготовку темы"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Дело до 2 минут — закрой сразу, чтобы не висело в голове.",
            categoryBadge = "Учеба • Быстрое",
            estimatedMinutes = 1,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.MECHANICAL,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 500L,
            createdAt = System.currentTimeMillis() - 3600000L * 4,
            aiDraftType = "Черновик сообщения",
            aiDraftContent = "«Привет! Запиши мою тему курсовой: Разработка мобильного GTD-ассистента со свайпами и голосовым вводом»",
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 15, 18, 0),
            scheduledTimeText = "18:00",
            contextTag = "phone"
        ),

        // 4. Один шаг (SINGLE_STEP) — Схема Room
        TaskEntity(
            id = "task-step-sbros-db",
            projectId = "project-sbros",
            orderInProject = 2,
            originalQuote = "Из речи: «...проверить схему Room для сохранения задач...»",
            title = "Проверить поля TaskEntity в Room Database",
            simplifiedSteps = listOf(
                "Открыть файл Entities.kt в Android Studio",
                "Сверить список полей с README"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Понятный шаг разработки на 10-15 минут.",
            categoryBadge = "Приложение Сброс • Шаг 2/4",
            estimatedMinutes = 15,
            shelf = ShelfType.BIG_THINGS,
            energyLevel = EnergyLevel.SINGLE_STEP,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 3000L,
            createdAt = System.currentTimeMillis() - 3600000L * 5,
            aiDraftType = "Схема Room",
            aiDraftContent = "@Entity(tableName = \"tasks\")\ndata class TaskEntity(@PrimaryKey val id: String, val title: String, val shelf: String)",
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 20, 11, 0),
            scheduledTimeText = "11:00",
            contextTag = "pc"
        ),

        // 5. Поток (FLOW) — Курсовая: Введение
        TaskEntity(
            id = "task-flow-coursework-intro",
            projectId = "project-coursework",
            orderInProject = 1,
            originalQuote = "Из речи: «...надо хотя бы введение к курсачу родить...»",
            title = "Написать черновик введения (3 абзаца)",
            simplifiedSteps = listOf(
                "Набросать 3 коротких тезиса своими словами",
                "Создать документ и написать заголовок главы"
            ),
            currentSimplifyIndex = 0,
            aiHint = "ИИ спрятал остальные главы, оставив только черновик на 20 минут.",
            categoryBadge = "Курсовая работа • Шаг 1/5",
            estimatedMinutes = 20,
            shelf = ShelfType.BIG_THINGS,
            energyLevel = EnergyLevel.FLOW,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 4000L,
            createdAt = System.currentTimeMillis() - 3600000L * 6,
            aiDraftType = "План введения",
            aiDraftContent = "План введения:\n1. Проблема когнитивного перегруза в сложных GTD-системах.\n2. Голосовой сбор мыслей (Mind Sweep).\n3. Декомпозиция задач с помощью LLM.",
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 17, 14, 0),
            scheduledTimeText = "14:00",
            contextTag = "pc"
        ),

        TaskEntity(
            id = "task-flow-coursework-outline",
            projectId = "project-coursework",
            orderInProject = 2,
            originalQuote = "Из речи: «...составить содержание и список глав...»",
            title = "Составить содержание и список глав",
            simplifiedSteps = listOf(
                "Написать названия трех глав",
                "Сверить со структурой методички"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Второй шаг курсовой, откроется после завершения введения.",
            categoryBadge = "Курсовая работа • Шаг 2/5",
            estimatedMinutes = 15,
            shelf = ShelfType.BIG_THINGS,
            energyLevel = EnergyLevel.FLOW,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 3600000L * 7,
            aiDraftType = "Структура",
            aiDraftContent = "Глава 1. Теоретические основы...\nГлава 2. Анализ и проектирование...\nГлава 3. Практическая реализация...",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "pc"
        ),

        // 6. Жду ответа (WAITING) — Научрук
        TaskEntity(
            id = "task-waiting-supervisor",
            projectId = null,
            orderInProject = 0,
            originalQuote = "Из речи: «...научрук обещал посмотреть первую главу...»",
            title = "Правки по первой главе от научрука",
            simplifiedSteps = listOf(
                "Проверить почту или Telegram",
                "Вежливо напомнить в четверг, если не ответит"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Дело на паузе, от тебя действий сейчас не требуется.",
            categoryBadge = "Курсовая работа",
            estimatedMinutes = 0,
            shelf = ShelfType.WAITING,
            energyLevel = EnergyLevel.MECHANICAL,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            aiDraftType = "Черновик напоминания",
            aiDraftContent = "«Здравствуйте! Подскажите, пожалуйста, удалось ли посмотреть черновик первой главы?»",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "phone"
        ),

        // 7. На потом и идеи (SOMEDAY) — Фильм
        TaskEntity(
            id = "task-someday-movie",
            projectId = null,
            orderInProject = 0,
            originalQuote = "Из речи: «...надо глянуть новый фильм на выходных...»",
            title = "Посмотреть новый фильм на выходных",
            simplifiedSteps = listOf(
                "Добавить в список «Буду смотреть» на Кинопоиске",
                "Глянуть трейлер на 1 минуту"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Приятная идея без дедлайнов и обязательств.",
            categoryBadge = "Отдых",
            estimatedMinutes = 0,
            shelf = ShelfType.SOMEDAY,
            energyLevel = EnergyLevel.MECHANICAL,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            aiDraftType = "Совет ИИ",
            aiDraftContent = "Хороший отдых восстанавливает силы. Выбери вечер субботы.",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "home"
        ),

        // 8. Справочник и заметки (REFERENCE) — ГОСТ
        TaskEntity(
            id = "task-ref-gost",
            projectId = null,
            orderInProject = 0,
            originalQuote = "Справочная информация",
            title = "Требования ГОСТ к оформлению списка литературы",
            simplifiedSteps = listOf(
                "Открыть сохраненную методичку",
                "Скопировать шаблон оформления источников"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Справочная заметка для быстрой сверки перед сдачей.",
            categoryBadge = "Курсовая работа",
            estimatedMinutes = 0,
            shelf = ShelfType.REFERENCE,
            energyLevel = EnergyLevel.SINGLE_STEP,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 4,
            aiDraftType = "Справка",
            aiDraftContent = "ГОСТ 7.0.5-2008: Фамилия И.О. Название книги. — Город: Издательство, Год. — 000 с.",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "pc"
        )
    )
}
