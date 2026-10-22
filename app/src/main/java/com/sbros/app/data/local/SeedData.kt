package com.sbros.app.data.local

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskDifficulty
import java.util.Calendar

object SeedData {

    // Valid RFC-4122 UUIDs for PostgreSQL compatibility
    const val PROJECT_COURSEWORK_ID = "00000001-0000-0000-0000-000000000001"
    const val PROJECT_SBROS_ID      = "00000001-0000-0000-0000-000000000002"

    const val TASK_BATTERIES_ID          = "00000002-0000-0000-0000-000000000001"
    const val TASK_TELEGRAM_TOPIC_ID     = "00000002-0000-0000-0000-000000000002"
    const val TASK_ROOM_DB_ID            = "00000002-0000-0000-0000-000000000003"
    const val TASK_COURSEWORK_INTRO_ID   = "00000002-0000-0000-0000-000000000004"
    const val TASK_COURSEWORK_OUTLINE_ID = "00000002-0000-0000-0000-000000000005"
    const val TASK_CAL_LAB_ID            = "00000002-0000-0000-0000-000000000006"
    const val TASK_CAL_DEMO_ID           = "00000002-0000-0000-0000-000000000007"
    const val TASK_CAL_EXAM_ID           = "00000002-0000-0000-0000-000000000008"
    const val TASK_CAL_COFFEE_ID         = "00000002-0000-0000-0000-000000000009"
    const val TASK_WAITING_SUPERVISOR_ID = "00000002-0000-0000-0000-000000000010"
    const val TASK_SOMEDAY_MOVIE_ID      = "00000002-0000-0000-0000-000000000011"
    const val TASK_REF_GOST_ID           = "00000002-0000-0000-0000-000000000012"

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

    val initialProjects = listOf(
        ProjectEntity(
            id = PROJECT_COURSEWORK_ID,
            title = "Курсовая работа",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        ProjectEntity(
            id = PROJECT_SBROS_ID,
            title = "Приложение Сброс",
            createdAt = System.currentTimeMillis() - 86400000L
        )
    )

    val initialTasks = listOf(
        // 1. Просто (EASY) — Заказ аккумуляторов
        TaskEntity(
            id = TASK_BATTERIES_ID,
            projectId = null,
            orderInProject = 0,
            originalQuote = "Очищено из речи: «...короче надо не забыть купить аккумуляторы АА...»",
            title = "Заказать аккумуляторы АА на маркетплейсе",
            simplifiedSteps = listOf(
                "Открыть маркетплейс и вбить «Аккумуляторы АА Ni-MH 4 шт»",
                "Положить первую подходящую упаковку в корзину"
            ),
            currentSimplifyIndex = 0,
            aiHint = "ИИ отделил это от мыслей про камеру в простую покупку.",
            categoryBadge = "Покупки и заказы • Просто",
            estimatedMinutes = 3,
            shelf = ShelfType.SIMPLE_STEPS,
            difficulty = TaskDifficulty.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 1000L,
            createdAt = System.currentTimeMillis() - 3600000L * 3,
            aiDraftType = "Рекомендация ИИ",
            aiDraftContent = "Поиск: Ni-MH AA 2100-2500 mAh (низкий саморазряд LSD, держат привод объектива без просадки напряжения).",
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 17, 19, 30),
            scheduledTimeText = "19:30",
            contextTag = "out",
            contextCategory = "Покупки и заказы",
            substeps = emptyList(),
            aiSuggestedSteps = listOf(
                "Открыть поиск маркетплейса и вбить «Аккумуляторы АА Ni-MH»",
                "Выбрать упаковку на 4 шт от 2000 mAh",
                "Оформить доставку в ближайший ПВЗ"
            ),
            aiAdvice = "Бери Ni-MH с низким саморазрядом (LSD) — они лучше держат ток."
        ),

        // 2. Просто (EASY) — Староста
        TaskEntity(
            id = TASK_TELEGRAM_TOPIC_ID,
            projectId = null,
            orderInProject = 0,
            originalQuote = "Очищено из речи: «...блин еще старосте тему скинуть надо...»",
            title = "Отправить старосте утвержденную тему работы в Telegram",
            simplifiedSteps = listOf(
                "Скопировать точное название темы из заметок",
                "Отправить сообщение старосте в личку"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Быстрое дело на 1-2 минуты, чтобы не висело в голове.",
            categoryBadge = "Учеба и вуз • Просто",
            estimatedMinutes = 2,
            shelf = ShelfType.SIMPLE_STEPS,
            difficulty = TaskDifficulty.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 2000L,
            createdAt = System.currentTimeMillis() - 3600000L * 2,
            aiDraftType = "Черновик сообщения",
            aiDraftContent = "«Привет! Запиши мою тему курсовой: Разработка мобильного GTD-ассистента со свайпами и голосовым вводом»",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "phone",
            contextCategory = "Учеба и вуз",
            substeps = emptyList(),
            aiSuggestedSteps = listOf(
                "Скопировать точное название темы из заметок",
                "Отправить сообщение старосте в личку"
            ),
            aiAdvice = "Шаблон: «Привет! Запиши мою тему: Разработка мобильного GTD-ассистента»"
        ),

        // 3. Средне (MEDIUM) — Схема Room
        TaskEntity(
            id = TASK_ROOM_DB_ID,
            projectId = PROJECT_SBROS_ID,
            orderInProject = 2,
            originalQuote = "Очищено из речи: «...надо в приложении базу данных поправить под контексты...»",
            title = "Настроить схему Room Database для хранения контекстов",
            simplifiedSteps = listOf(
                "Добавить поля difficulty и contextCategory в TaskEntity",
                "Обновить DAO-запросы фильтрации"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Понятный шаг разработки на 15 минут.",
            categoryBadge = "Разработка и код • Средне",
            estimatedMinutes = 15,
            shelf = ShelfType.BIG_THINGS,
            difficulty = TaskDifficulty.MEDIUM,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 3000L,
            createdAt = System.currentTimeMillis() - 3600000L * 5,
            aiDraftType = "Схема Room",
            aiDraftContent = "Храни шаги декомпозиции в связанной таблице или через TypeConverter.",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "pc",
            contextCategory = "Разработка и код",
            substeps = emptyList(),
            aiSuggestedSteps = listOf(
                "Добавить поля difficulty и contextCategory в TaskEntity",
                "Создать таблицу SubtaskEntity для хранения шагов декомпозиции",
                "Обновить DAO-запросы фильтрации"
            ),
            aiAdvice = "Храни шаги декомпозиции в связанной таблице или через TypeConverter."
        ),

        // 4. Сложно (HARD) — Курсовая: Введение
        TaskEntity(
            id = TASK_COURSEWORK_INTRO_ID,
            projectId = PROJECT_COURSEWORK_ID,
            orderInProject = 1,
            originalQuote = "Очищено из речи: «...вообще не знаю когда садиться за первую главу курсача...»",
            title = "Написать черновик введения (3 абзаца)",
            simplifiedSteps = listOf(
                "Набросать 3 коротких тезиса своими словами",
                "Создать документ и написать заголовок главы"
            ),
            currentSimplifyIndex = 0,
            aiHint = "ИИ спрятал остальные главы, оставив только черновик на 20 минут.",
            categoryBadge = "Учеба и вуз • Сложно",
            estimatedMinutes = 20,
            shelf = ShelfType.BIG_THINGS,
            difficulty = TaskDifficulty.HARD,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 4000L,
            createdAt = System.currentTimeMillis() - 3600000L * 6,
            aiDraftType = "План введения",
            aiDraftContent = "План введения:\n1. Проблема когнитивного перегруза в сложных GTD-системах.\n2. Голосовой сбор мыслей (Mind Sweep).\n3. Декомпозиция задач с помощью LLM.",
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 17, 14, 0),
            scheduledTimeText = "14:00",
            contextTag = "pc",
            contextCategory = "Учеба и вуз",
            substeps = emptyList(),
            aiSuggestedSteps = listOf(
                "Составить оглавление из 3 подразделов первой главы",
                "Написать раздел 1.1: Обзор методологии GTD и существующих решений",
                "Написать раздел 1.2: Постановка задачи голосовой обработки через LLM"
            ),
            aiAdvice = "Начни с раздела 1.1 — там можно опереться на базовую схему Дэвида Аллена."
        ),

        TaskEntity(
            id = TASK_COURSEWORK_OUTLINE_ID,
            projectId = PROJECT_COURSEWORK_ID,
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
            difficulty = TaskDifficulty.HARD,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 3600000L * 7,
            aiDraftType = "Структура",
            aiDraftContent = "Глава 1. Теоретические основы...\nГлава 2. Анализ и проектирование...\nГлава 3. Практическая реализация...",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "pc",
            contextCategory = "Учеба и вуз",
            substeps = emptyList(),
            aiSuggestedSteps = listOf(
                "Выписать 3 основных раздела",
                "Сверить с требованиями кафедры"
            ),
            aiAdvice = "Держись структуры из методических указаний."
        ),

        // Календарь: 14 Октября (1 задача)
        TaskEntity(
            id = TASK_CAL_LAB_ID,
            projectId = null,
            orderInProject = 0,
            originalQuote = "Назначено в календаре",
            title = "Сдать отчет по лабораторной",
            simplifiedSteps = listOf("Прикрепить PDF в личный кабинет"),
            currentSimplifyIndex = 0,
            aiHint = "Дедлайн сдачи лабораторной работы.",
            categoryBadge = "Учеба и вуз • Средне",
            estimatedMinutes = 10,
            shelf = ShelfType.SIMPLE_STEPS,
            difficulty = TaskDifficulty.MEDIUM,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 14, 18, 0),
            scheduledTimeText = "18:00",
            contextTag = "pc",
            contextCategory = "Учеба и вуз"
        ),

        // Календарь: 22 Октября (3 задачи)
        TaskEntity(
            id = TASK_CAL_DEMO_ID,
            projectId = null,
            orderInProject = 0,
            originalQuote = "Назначено в календаре",
            title = "Показать прототип приложения",
            simplifiedSteps = listOf("Собрать APK и подготовить слайды"),
            currentSimplifyIndex = 0,
            aiHint = "Демонстрация рабочего прототипа на встрече.",
            categoryBadge = "Разработка и код • Средне",
            estimatedMinutes = 30,
            shelf = ShelfType.SIMPLE_STEPS,
            difficulty = TaskDifficulty.MEDIUM,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 22, 10, 0),
            scheduledTimeText = "10:00",
            contextTag = "pc",
            contextCategory = "Разработка и код"
        ),
        TaskEntity(
            id = TASK_CAL_EXAM_ID,
            projectId = null,
            orderInProject = 0,
            originalQuote = "Назначено в календаре",
            title = "Зачет по моделированию",
            simplifiedSteps = listOf("Повторить формулы и билеты"),
            currentSimplifyIndex = 0,
            aiHint = "Зачетная неделя.",
            categoryBadge = "Учеба и вуз • Сложно",
            estimatedMinutes = 60,
            shelf = ShelfType.SIMPLE_STEPS,
            difficulty = TaskDifficulty.HARD,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 22, 15, 0),
            scheduledTimeText = "15:00",
            contextTag = "pc",
            contextCategory = "Учеба и вуз"
        ),
        TaskEntity(
            id = TASK_CAL_COFFEE_ID,
            projectId = null,
            orderInProject = 0,
            originalQuote = "Назначено в календаре",
            title = "Купить кофе и фильтр-пакеты",
            simplifiedSteps = listOf("Зайти в кофейню рядом с домом"),
            currentSimplifyIndex = 0,
            aiHint = "Пополнение запасов кофе.",
            categoryBadge = "Покупки и заказы • Просто",
            estimatedMinutes = 5,
            shelf = ShelfType.SIMPLE_STEPS,
            difficulty = TaskDifficulty.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            scheduledDateMillis = getCalTimestamp(2026, Calendar.OCTOBER, 22, 20, 0),
            scheduledTimeText = "20:00",
            contextTag = "out",
            contextCategory = "Покупки и заказы"
        ),

        // Полка: Жду ответа (WAITING) — Научрук
        TaskEntity(
            id = TASK_WAITING_SUPERVISOR_ID,
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
            categoryBadge = "Учеба и вуз • Ожидание",
            estimatedMinutes = 0,
            shelf = ShelfType.WAITING,
            difficulty = TaskDifficulty.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            aiDraftType = "Черновик напоминания",
            aiDraftContent = "«Здравствуйте! Подскажите, пожалуйста, удалось ли посмотреть черновик первой главы?»",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "phone",
            contextCategory = "Учеба и вуз",
            substeps = emptyList(),
            aiAdvice = "Напомни в четверг, если научрук не ответит до вечера."
        ),

        // Полка: На потом и идеи (SOMEDAY) — Фильм
        TaskEntity(
            id = TASK_SOMEDAY_MOVIE_ID,
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
            categoryBadge = "Дом и быт • Отдых",
            estimatedMinutes = 0,
            shelf = ShelfType.SOMEDAY,
            difficulty = TaskDifficulty.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            aiDraftType = "Совет ИИ",
            aiDraftContent = "Хороший отдых восстанавливает силы. Выбери вечер субботы.",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "home",
            contextCategory = "Дом и быт",
            substeps = emptyList(),
            aiAdvice = "Хороший отдых восстанавливает силы."
        ),

        // Полка: Справочник и заметки (REFERENCE) — ГОСТ
        TaskEntity(
            id = TASK_REF_GOST_ID,
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
            categoryBadge = "Учеба и вуз • Справка",
            estimatedMinutes = 0,
            shelf = ShelfType.REFERENCE,
            difficulty = TaskDifficulty.MEDIUM,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L * 4,
            aiDraftType = "Справка",
            aiDraftContent = "ГОСТ 7.0.5-2008: Фамилия И.О. Название книги. — Город: Издательство, Год. — 000 с.",
            scheduledDateMillis = null,
            scheduledTimeText = null,
            contextTag = "pc",
            contextCategory = "Учеба и вуз",
            substeps = emptyList(),
            aiAdvice = "ГОСТ 7.0.5-2008 регламентирует библиографические ссылки."
        )
    )
}
