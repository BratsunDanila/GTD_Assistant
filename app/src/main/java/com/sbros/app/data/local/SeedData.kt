package com.sbros.app.data.local

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import java.util.Calendar

object SeedData {

    private fun getDayTimestamp(offsetDays: Int, hour: Int, minute: Int): Long {
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
            createdAt = System.currentTimeMillis() - 3600000L * 3,
            aiDraftType = "Поисковый фильтр ИИ",
            aiDraftContent = "Рекомендация: бери Ni-MH от 2000 mAh (например, GP или Robiton) — они держат вспышку и объектив без просадки напряжения.",
            scheduledDateMillis = getDayTimestamp(1, 12, 0),
            scheduledTimeText = "12:00"
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
            createdAt = System.currentTimeMillis() - 3600000L * 2,
            aiDraftType = "Быстрая подсказка",
            aiDraftContent = "Проверь, чтобы оба аккумулятора были из одной пары перед установкой в слоты зарядного устройства.",
            scheduledDateMillis = null,
            scheduledTimeText = null
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
            createdAt = System.currentTimeMillis() - 3600000L * 4,
            aiDraftType = "Черновик сообщения",
            aiDraftContent = "«Привет! Запиши мою тему курсовой: Разработка мобильного GTD-ассистента со свайпами и голосовым вводом»",
            scheduledDateMillis = getDayTimestamp(0, 16, 0),
            scheduledTimeText = "16:00"
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
            createdAt = System.currentTimeMillis() - 3600000L * 5,
            aiDraftType = "Стартовый каркас текста",
            aiDraftContent = "1. Проблема перегруза в классических таск-менеджерах.\n2. Снижение порога входа за счет голосового потока (Mind Sweep).\n3. Автоматическая декомпозиция задач через LLM.",
            scheduledDateMillis = getDayTimestamp(0, 14, 0),
            scheduledTimeText = "14:00"
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
            createdAt = System.currentTimeMillis() - 3600000L * 5,
            aiDraftType = "Стартовый план",
            aiDraftContent = "Глава 1. Анализ методологий GTD.\nГлава 2. Архитектура и стек Compose + Room.\nГлава 3. Оценка когнитивной нагрузки пользователя.",
            scheduledDateMillis = null,
            scheduledTimeText = null
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
            createdAt = System.currentTimeMillis() - 3600000L * 6,
            aiDraftType = "Совет ИИ",
            aiDraftContent = "Тональные палитры Material 3 обеспечивают комфортный контраст в светлой и тёмной темах.",
            scheduledDateMillis = null,
            scheduledTimeText = null
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
            createdAt = System.currentTimeMillis() - 3600000L * 7,
            aiDraftType = "Черновик напоминания",
            aiDraftContent = "«Здравствуйте! Подскажите, пожалуйста, удалось ли посмотреть черновик главы?»",
            scheduledDateMillis = getDayTimestamp(-1, 14, 0),
            scheduledTimeText = "14:00"
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
            createdAt = System.currentTimeMillis() - 3600000L * 8,
            aiDraftType = "План отдыха",
            aiDraftContent = "Дюна 2 идет 2 ч 46 мин. Отличный повод заказать попкорн и полностью отключить уведомления.",
            scheduledDateMillis = getDayTimestamp(0, 20, 0),
            scheduledTimeText = "20:00"
        ),

        // 9. Календарная задача: Лабораторная (вчера/четверг)
        TaskEntity(
            id = "task-cal-lab-report",
            projectId = null,
            orderInProject = 0,
            originalQuote = "сдать отчет по лабораторной в четверг в 18:00",
            title = "Сдать отчет по лабораторной",
            simplifiedSteps = listOf(
                "Проверить титульный лист",
                "Загрузить в личный кабинет студента"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Дедлайн зафиксирован в календаре.",
            categoryBadge = "📚 Учеба • Дедлайн",
            estimatedMinutes = 15,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.FOCUS,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L,
            aiDraftType = "Чек-лист проверки",
            aiDraftContent = "1. Номер варианта.\n2. Выводы по работе.\n3. Скриншоты работы программы.",
            scheduledDateMillis = getDayTimestamp(-1, 18, 0),
            scheduledTimeText = "18:00"
        ),

        // 10. Календарная задача: Кафедра (понедельник)
        TaskEntity(
            id = "task-cal-department",
            projectId = null,
            orderInProject = 0,
            originalQuote = "в понедельник в 10:00 утвердить финальную тему",
            title = "Утвердить финальную тему на кафедре",
            simplifiedSteps = listOf(
                "Взять подписанное заявление",
                "Зайти в кабинет 402"
            ),
            currentSimplifyIndex = 0,
            aiHint = "Критический дедлайн по курсовой.",
            categoryBadge = "🔥 Жесткий дедлайн",
            estimatedMinutes = 10,
            shelf = ShelfType.SIMPLE_STEPS,
            energyLevel = EnergyLevel.FIVE_MIN,
            isCompleted = false,
            skippedCount = 0,
            lastShownAt = 0L,
            createdAt = System.currentTimeMillis() - 86400000L,
            aiDraftType = "Шаблон заявления",
            aiDraftContent = "«Прошу утвердить тему курсовой работы: Разработка интеллектуального таск-ассистента на Android.»",
            scheduledDateMillis = getDayTimestamp(3, 10, 0),
            scheduledTimeText = "10:00"
        )
    )
}
