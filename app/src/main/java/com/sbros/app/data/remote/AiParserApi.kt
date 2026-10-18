package com.sbros.app.data.remote

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.remote.dto.DumpResponseDto
import com.sbros.app.data.remote.dto.ParsedItemDto
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface AiParserApi {
    suspend fun parseTranscript(
        rawTranscript: String,
        existingProjects: List<ProjectEntity>
    ): DumpResponseDto
}

@Singleton
class AiParserApiImpl @Inject constructor() : AiParserApi {

    override suspend fun parseTranscript(
        rawTranscript: String,
        existingProjects: List<ProjectEntity>
    ): DumpResponseDto {
        val trimmed = rawTranscript.trim()
        if (trimmed.isBlank()) {
            return DumpResponseDto(emptyList())
        }

        // Локальный умный GTD-парсер (Mock/Offline режим)
        val items = mutableListOf<ParsedItemDto>()

        // 1. Фильтрация эмоционального шума (Venting)
        val sentences = trimmed.split(Regex("[,.;\n]+|\\s+и\\s+|\\s+еще\\s+|\\s+также\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (phrase in sentences) {
            val lower = phrase.lowercase(Locale.ROOT)

            // Пропускаем чистые эмоции и жалобы
            if (isPureEmotion(lower)) {
                continue
            }

            val item = classifyPhrase(phrase, lower, existingProjects)
            if (item != null) {
                items.add(item)
            }
        }

        // Если фраза не разбилась, обрабатываем как единую мысль
        if (items.isEmpty()) {
            val lower = trimmed.lowercase(Locale.ROOT)
            if (!isPureEmotion(lower)) {
                classifyPhrase(trimmed, lower, existingProjects)?.let { items.add(it) }
            }
        }

        return DumpResponseDto(items)
    }

    private fun isPureEmotion(lower: String): Boolean {
        val emotionWords = listOf(
            "как же я устал", "я устал", "ничего не успеваю", "блин", "капец",
            "жесть", "сил нет", "не могу больше", "кошмар", "ужас", "бесит",
            "голова кругом", "каша в голове"
        )
        return emotionWords.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }
    }

    private fun classifyPhrase(
        original: String,
        lower: String,
        existingProjects: List<ProjectEntity>
    ): ParsedItemDto? {
        val cleanPhrase = original
            .replace(Regex("^(надо|нужно|хочу|не забыть|хотел бы|было бы круто)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        if (cleanPhrase.isBlank()) return null

        val timeText = extractTime(lower)
        val dateMillis = extractDateMillis(lower)

        // Определение смыслового Контекста по sbros_v8.html
        val contextCategory = when {
            lower.contains("купить") || lower.contains("заказать") || lower.contains("батарейк") ||
                    lower.contains("аккумулятор") || lower.contains("маркетплейс") || lower.contains("магазин") ||
                    lower.contains("кофе") || lower.contains("доставк") || lower.contains("пвз") -> "Покупки и заказы"

            lower.contains("старост") || lower.contains("курс") || lower.contains("диплом") ||
                    lower.contains("вуз") || lower.contains("универ") || lower.contains("лаб") ||
                    lower.contains("зачет") || lower.contains("научрук") || lower.contains("тема") ||
                    lower.contains("стать") || lower.contains("учеб") -> "Учеба и вуз"

            lower.contains("код") || lower.contains("room") || lower.contains("баз") ||
                    lower.contains("приложен") || lower.contains("разработк") || lower.contains("git") ||
                    lower.contains("прототип") || lower.contains("схем") || lower.contains("api") -> "Разработка и код"

            else -> "Дом и быт"
        }

        // Определение сложности по sbros_v8.html: Просто (EASY) / Средне (MEDIUM) / Сложно (HARD)
        val difficulty = when {
            lower.contains("курс") || lower.contains("диплом") || lower.contains("глав") ||
                    lower.contains("введение") || lower.contains("сложн") || lower.contains("зачет") ||
                    lower.contains("проект") -> "HARD"

            lower.contains("старост") || lower.contains("сдать") || lower.contains("отчет") || lower.contains("настроить") ||
                    lower.contains("схем") || lower.contains("код") || lower.contains("разработк") ||
                    lower.contains("прототип") -> "MEDIUM"

            else -> "EASY"
        }

        val energyLevel = when (difficulty) {
            "HARD" -> "FLOW"
            "MEDIUM" -> "SINGLE_STEP"
            else -> "MECHANICAL"
        }

        // Подсказки шагов декомпозиции (для кнопки "Разбить автоматически с помощью ИИ")
        val suggestedSteps = when (contextCategory) {
            "Покупки и заказы" -> listOf(
                "Открыть поиск маркетплейса и вбить точное название",
                "Выбрать подходящий вариант по отзывам и цене",
                "Оформить заказ в ближайший ПВЗ"
            )
            "Учеба и вуз" -> listOf(
                "Открыть рабочие материалы или конспекты",
                "Набросать ключевые пункты или тезисы",
                "Сверить результат с требованиями и сохранить"
            )
            "Разработка и код" -> listOf(
                "Сформулировать задачу и проверить текущую ветку в Git",
                "Написать реализацию и запустить локальные тесты",
                "Сделать коммит и зафиксировать прогресс"
            )
            else -> listOf(
                "Подготовить всё необходимое для старта",
                "Выполнить основную часть задачи",
                "Проверить итог и убрать рабочее место"
            )
        }

        val aiAdvice = when (contextCategory) {
            "Покупки и заказы" -> "Выбирай проверенного продавца с быстрой доставкой и гарантией."
            "Учеба и вуз" -> "Начни с простого драфта из 2-3 предложений — это снимет барьер начала."
            "Разработка и код" -> "Делай частые коммиты с понятными сообщениями и проверяй сборку."
            else -> "Раздели дело на короткие понятные действия и сделай первый шаг."
        }

        // 1. Полка: На потом и отдых (SOMEDAY)
        if (lower.contains("кино") || lower.contains("фильм") || lower.contains("сериал") ||
            lower.contains("книг") || lower.contains("поиграть") || lower.contains("выходн") ||
            lower.contains("отдых") || lower.contains("на потом")
        ) {
            return ParsedItemDto(
                originalQuote = "Из речи: «$original»",
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Добавить в закладки или заметки",
                    "Найти трейлер или описание на 1 минуту"
                ),
                aiHint = "Приятный план на отдых без дедлайнов и обязательств.",
                categoryBadge = if (timeText != null || dateMillis != null) "Календарь • Отдых" else "Идея • Отдых",
                estimatedMinutes = 0,
                shelf = "SOMEDAY",
                energyLevel = energyLevel,
                difficulty = difficulty,
                aiDraftType = "Совет ИИ",
                aiDraftContent = "Хороший отдых восстанавливает силы. Выбери подходящее время и не думай о делах.",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText,
                contextTag = "home",
                contextCategory = contextCategory,
                aiSuggestedSteps = suggestedSteps,
                aiAdvice = aiAdvice
            )
        }

        // 2. Полка: Жду ответа (WAITING)
        if (lower.contains("жд") || lower.contains("научрук") || lower.contains("ответ") ||
            lower.contains("проверк") || lower.contains("правк")
        ) {
            return ParsedItemDto(
                originalQuote = "Из речи: «$original»",
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Проверить статус или почту",
                    "Напомнить вежливым сообщением, если не ответят"
                ),
                aiHint = "Дело на паузе, от тебя сейчас действий не требуется.",
                categoryBadge = if (timeText != null || dateMillis != null) "Календарь • Ожидание" else "Ожидание • Контроль",
                estimatedMinutes = 0,
                shelf = "WAITING",
                energyLevel = energyLevel,
                difficulty = difficulty,
                aiDraftType = "Черновик напоминания",
                aiDraftContent = "«Здравствуйте! Подскажите, пожалуйста, удалось ли посмотреть материалы по $cleanPhrase?»",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText,
                contextTag = "phone",
                contextCategory = contextCategory,
                aiSuggestedSteps = suggestedSteps,
                aiAdvice = aiAdvice
            )
        }

        // 3. Полка: Большие дела (BIG_THINGS)
        if (difficulty == "HARD" || lower.contains("курсач") || lower.contains("курсов") ||
            lower.contains("диплом") || lower.contains("проект")
        ) {
            val matchedProj = existingProjects.firstOrNull {
                val projLower = it.title.lowercase(Locale.ROOT)
                lower.contains(projLower) ||
                        (projLower.contains("курс") && (lower.contains("курс") || lower.contains("курсач"))) ||
                        (projLower.contains("диплом") && lower.contains("диплом")) ||
                        (projLower.contains("сброс") && lower.contains("сброс"))
            }
            val projTitle = matchedProj?.title ?: when {
                lower.contains("курс") -> "Курсовая работа"
                lower.contains("диплом") -> "Дипломная работа"
                else -> cleanPhrase.take(30)
            }

            return ParsedItemDto(
                originalQuote = "Из речи: «$original»",
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Открыть документ и набросать 3 коротких тезиса",
                    "Просто создать пустой файл и написать заголовок"
                ),
                aiHint = "ИИ выделил задачу и оценил как сложную для фокуса.",
                categoryBadge = if (timeText != null || dateMillis != null) "Дедлайн • $projTitle" else "$contextCategory • Сложно",
                estimatedMinutes = 20,
                shelf = "BIG_THINGS",
                energyLevel = energyLevel,
                difficulty = difficulty,
                matchedProjectId = matchedProj?.id,
                newProjectTitle = if (matchedProj == null) projTitle else null,
                aiDraftType = "Стартовый план",
                aiDraftContent = "1. Сформулировать 3 ключевых тезиса.\n2. Набросать черновой каркас введения.\n3. Сверить с методическими указаниями.",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText,
                contextTag = "pc",
                contextCategory = contextCategory,
                aiSuggestedSteps = suggestedSteps,
                aiAdvice = aiAdvice
            )
        }

        // 4. Полка: Простые шаги (SIMPLE_STEPS)
        val isBuy = contextCategory == "Покупки и заказы"
        return ParsedItemDto(
            originalQuote = "Из речи: «$original»",
            title = cleanPhrase,
            simplifiedSteps = listOf(
                "Открыть нужное приложение или диалог",
                "Сделать первое простое действие на 30 секунд"
            ),
            aiHint = "Понятная задача, готовая к быстрому выполнению.",
            categoryBadge = if (timeText != null || dateMillis != null) "Календарь: ${timeText ?: "срок"}" else "$contextCategory • ${if (difficulty == "EASY") "Просто" else "Средне"}",
            estimatedMinutes = if (isBuy) 3 else 5,
            shelf = "SIMPLE_STEPS",
            energyLevel = energyLevel,
            difficulty = difficulty,
            aiDraftType = if (isBuy) "Рекомендация ИИ" else "Черновик сообщения",
            aiDraftContent = if (isBuy) "Поиск: проверенный товар с высоким рейтингом и быстрой доставкой."
            else "«Привет! Пишу по поводу $cleanPhrase. Всё в процессе, скоро пришлю детали.»",
            scheduledDateMillis = dateMillis,
            scheduledTimeText = timeText,
            contextTag = if (isBuy) "out" else if (lower.contains("телефон") || lower.contains("чат")) "phone" else "home",
            contextCategory = contextCategory,
            aiSuggestedSteps = suggestedSteps,
            aiAdvice = aiAdvice
        )
    }

    private fun extractTime(lower: String): String? {
        val timeRegex = Regex("(?:в\\s+)?([0-2]?[0-9][:.]00|[0-2]?[0-9]:[0-5][0-9])")
        val match = timeRegex.find(lower) ?: return null
        val raw = match.groupValues[1]
        return raw.replace(".", ":")
    }

    private fun extractDateMillis(lower: String): Long? {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        when {
            lower.contains("сегодня") -> return cal.timeInMillis
            lower.contains("завтра") -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                return cal.timeInMillis
            }
            lower.contains("послезавтра") -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
                return cal.timeInMillis
            }
            lower.contains("понедельник") || lower.contains("пн") -> return getNextDayOfWeek(java.util.Calendar.MONDAY)
            lower.contains("вторник") || lower.contains("вт") -> return getNextDayOfWeek(java.util.Calendar.TUESDAY)
            lower.contains("сред") || lower.contains("ср") -> return getNextDayOfWeek(java.util.Calendar.WEDNESDAY)
            lower.contains("четверг") || lower.contains("чт") -> return getNextDayOfWeek(java.util.Calendar.THURSDAY)
            lower.contains("пятниц") || lower.contains("пт") -> return getNextDayOfWeek(java.util.Calendar.FRIDAY)
            lower.contains("суббот") || lower.contains("сб") -> return getNextDayOfWeek(java.util.Calendar.SATURDAY)
            lower.contains("воскресень") || lower.contains("вс") -> return getNextDayOfWeek(java.util.Calendar.SUNDAY)
        }
        return null
    }

    private fun getNextDayOfWeek(dayOfWeek: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK)
        var daysDiff = dayOfWeek - currentDay
        if (daysDiff < 0) daysDiff += 7
        cal.add(java.util.Calendar.DAY_OF_YEAR, daysDiff)
        return cal.timeInMillis
    }
}
