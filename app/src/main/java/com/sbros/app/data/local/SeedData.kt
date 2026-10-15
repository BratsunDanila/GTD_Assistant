package com.sbros.app.data.local

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType

object SeedData {

    val initialProjects = listOf(
        ProjectEntity(
            id = "project-coursework",
            title = "Курсовая работа",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        ProjectEntity(
            id = "project-sbros",
            title = "Приложение «Сброс»",
            createdAt = System.currentTimeMillis() - 86400000L
        )
    )

    val initialTasks = listOf(
        // 1. Легкое (EASY) — Заказ батареек
        TaskEntity(
            id = "task-easy-batteries-buy",
            projectId = null,
            orderInProject = 0,
            originalQuote = "надо купить батарейки для камеры",
            title = "Заказать аккумуляторы АА на маркетплейсе",
            simplifiedSteps = listOf(
                "Открыть приложение маркетплейса и вбить в поиск «Аккумуляторы АА»",
                "Кинуть первую подходящую упаковку в корзину"
            ),
            currentSimplifyIndex = 0,
            aiHint = "ИИ отделил это от мыслей про камеру в простую покупку.",
            categoryBadge = "Бытовое • Не требует фокуса",
            estimatedMinutes = 3,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 1000L,
            createdAt = System.currentTimeMillis() - 3600000L * 3
        ),

        // 2. Легкое (EASY) — Зарядка батареек
        TaskEntity(
            id = "task-easy-batteries-charge",
            projectId = null,
            orderInProject = 0,
            originalQuote = "батарейки валяются разряженные",
            title = "Поставить старые батарейки на зарядку",
            simplifiedSteps = listOf(
                "Просто достать зарядник из ящика на стол",
                "Вставить одну батарейку в слот"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Легкое физическое действие, чтобы переключиться.",
            categoryBadge = "Дом • Рутина",
            estimatedMinutes = 2,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 2000L,
            createdAt = System.currentTimeMillis() - 3600000L * 2
        ),

        // 3. На 5 мин (FIVE_MIN) — Сообщение старосте
        TaskEntity(
            id = "task-quick-telegram",
            projectId = null,
            orderInProject = 0,
            originalQuote = "старосте надо тему работы скинуть блин",
            title = "Отправить старосте тему работы в Telegram",
            simplifiedSteps = listOf(
                "Открыть диалог со старостой и скопировать туда название темы",
                "Написать «Привет, тема: ...»"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Быстрое дело, которое сразу освободит голову.",
            categoryBadge = "На пару минут • Мессенджер",
            estimatedMinutes = 1,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.FIVE_MIN,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 1000L,
            createdAt = System.currentTimeMillis() - 3600000L * 4
        ),

        // 4. Поработать (FOCUS) — Курсовая: 1-й шаг
        TaskEntity(
            id = "task-focus-coursework-intro",
            projectId = "project-coursework",
            orderInProject = 1,
            originalQuote = "надо хотя бы за курсач сесть",
            title = "Написать черновик введения (3 абзаца)",
            simplifiedSteps = listOf(
                "Открыть документ и написать 3 коротких буллита: о чем вообще работа",
                "Просто создать пустой файл и написать заголовок на первой странице"
            ),
            currentSimplifyIndex = 0,
            aiHint = "ИИ спрятал остальной курсач, чтобы он не давил.",
            categoryBadge = "Большое дело: Курсовая",
            estimatedMinutes = 20,
            shelf = ShelfType.BIG_THINGS,
            energyLevel = EnergyLevel.FOCUS,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 1000L,
            createdAt = System.currentTimeMillis() - 3600000L * 5
        ),

        // 5. Поработать (FOCUS) — Курсовая: 2-й шаг (скрыт внутри проекта)
        TaskEntity(
            id = "task-focus-coursework-plan",
            projectId = "project-coursework",
            orderInProject = 2,
            originalQuote = "сделать план глав для курсача",
            title = "Составить содержание и список глав",
            simplifiedSteps = listOf(
                "Набросать 3 названия глав",
                "Скопировать шаблон оглавления из методички"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Второй шаг курсовой — пока скрыт, чтобы не пугать объемом.",
            categoryBadge = "Большое дело: Курсовая",
            estimatedMinutes = 25,
            shelf = ShelfType.BIG_THINGS,
            energyLevel = EnergyLevel.FOCUS,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 3600000L * 5
        ),

        // 6. Поработать (FOCUS) — Приложение «Сброс»: 1-й шаг
        TaskEntity(
            id = "task-focus-sbros-palette",
            projectId = "project-sbros",
            orderInProject = 1,
            originalQuote = "надо определиться с цветами в приложении",
            title = "Выбрать базовую палитру M3",
            simplifiedSteps = listOf(
                "Посмотреть 4 палитры на стенде",
                "Выбрать Шалфей как основной"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Первый шаг по проекту приложения.",
            categoryBadge = "Большое дело: Сброс",
            estimatedMinutes = 15,
            shelf = ShelfType.BIG_THINGS,
            energyLevel = EnergyLevel.FOCUS,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 2000L,
            createdAt = System.currentTimeMillis() - 3600000L * 6
        ),

        // 7. Жду от других (WAITING)
        TaskEntity(
            id = "task-waiting-supervisor",
            projectId = null,
            orderInProject = 0,
            originalQuote = "научрук обещал посмотреть черновик до четверга",
            title = "Правки по первой главе от научрука",
            simplifiedSteps = listOf(
                "Проверить почту раз в день",
                "Напомнить, если не ответит до четверга"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Дело на паузе, от тебя действий сейчас не требуется.",
            categoryBadge = "Ожидание • Учеба",
            estimatedMinutes = 0,
            shelf = ShelfType.WAITING,
            energyLevel = EnergyLevel.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 3600000L * 7
        ),

        // 8. На потом и отдых (SOMEDAY)
        TaskEntity(
            id = "task-someday-dune",
            projectId = null,
            orderInProject = 0,
            originalQuote = "в пятницу надо фильм глянуть или сериал какой",
            title = "Посмотреть фильм «Дюна 2» в пятницу",
            simplifiedSteps = listOf(
                "Добавить в закладки онлайн-кинотеатра",
                "Купить попкорн"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Приятный план на отдых без обязательств.",
            categoryBadge = "Идея • Отдых",
            estimatedMinutes = 0,
            shelf = ShelfType.SOMEDAY,
            energyLevel = EnergyLevel.EASY,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 3600000L * 8
        )
    )
}
