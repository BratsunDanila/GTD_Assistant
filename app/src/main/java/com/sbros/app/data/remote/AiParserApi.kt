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
            "жесть", "сил нет", "не могу больше", "кошмар", "ужас", "бесит"
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

        // 1. Полка: На потом и отдых (SOMEDAY)
        if (lower.contains("кино") || lower.contains("фильм") || lower.contains("сериал") ||
            lower.contains("книг") || lower.contains("поиграть") || lower.contains("выходн") ||
            lower.contains("отдых") || lower.contains("на потом")
        ) {
            return ParsedItemDto(
                originalQuote = original,
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Добавить в закладки или заметки",
                    "Найти трейлер или описание на 1 минуту"
                ),
                aiHint = "Приятный план на отдых без дедлайнов и обязательств.",
                categoryBadge = if (timeText != null || dateMillis != null) "🍿 Календарь • Отдых" else "Идея • Отдых",
                estimatedMinutes = 0,
                shelf = "SOMEDAY",
                energyLevel = "EASY",
                aiDraftType = "Совет ИИ",
                aiDraftContent = "Хороший отдых восстанавливает силы. Выбери подходящее время и не думай о делах.",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText
            )
        }

        // 2. Полка: Жду от других (WAITING)
        if (lower.contains("жд") || lower.contains("научрук") || lower.contains("доставк") ||
            lower.contains("ответ") || lower.contains("проверк") || lower.contains("правк")
        ) {
            return ParsedItemDto(
                originalQuote = original,
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Проверить статус или почту",
                    "Напомнить вежливым сообщением, если не ответят"
                ),
                aiHint = "Дело на паузе, от тебя сейчас действий не требуется.",
                categoryBadge = if (timeText != null || dateMillis != null) "📅 Календарь • Ожидание" else "Ожидание • Контроль",
                estimatedMinutes = 0,
                shelf = "WAITING",
                energyLevel = "EASY",
                aiDraftType = "Черновик напоминания",
                aiDraftContent = "«Здравствуйте! Подскажите, пожалуйста, удалось ли посмотреть материалы по $cleanPhrase?»",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText
            )
        }

        // 3. Полка: Большие дела (BIG_THINGS)
        if (lower.contains("курсач") || lower.contains("курсов") || lower.contains("диплом") ||
            lower.contains("проект") || lower.contains("стать") || lower.contains("ремонт") ||
            lower.contains("переезд") || lower.contains("сдать") || lower.contains("лаб")
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
                originalQuote = original,
                title = "Сделать первый шаг по теме: $cleanPhrase",
                simplifiedSteps = listOf(
                    "Открыть документ и набросать 3 коротких тезиса",
                    "Просто создать пустой файл и написать заголовок"
                ),
                aiHint = "ИИ спрятал остальной проект, оставив только первый шаг на 15-20 минут.",
                categoryBadge = if (timeText != null || dateMillis != null) "📅 Дедлайн • $projTitle" else "Большое дело: $projTitle",
                estimatedMinutes = 20,
                shelf = "BIG_THINGS",
                energyLevel = "FOCUS",
                matchedProjectId = matchedProj?.id,
                newProjectTitle = if (matchedProj == null) projTitle else null,
                aiDraftType = "Стартовый план",
                aiDraftContent = "1. Сформулировать 3 ключевых тезиса.\n2. Набросать черновой каркас введения.\n3. Сверить с методическими указаниями.",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText
            )
        }

        // 4. Полка: Простые шаги -> На 5 минут (FIVE_MIN)
        if (lower.contains("написать") || lower.contains("скинуть") || lower.contains("ответить") ||
            lower.contains("позвонить") || lower.contains("переслать") || lower.contains("сообщ") ||
            lower.contains("старост") || lower.contains("telegram") || lower.contains("чат") ||
            lower.contains("созвон")
        ) {
            return ParsedItemDto(
                originalQuote = original,
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Открыть нужный диалог в мессенджере",
                    "Отправить короткое сообщение «Привет, скину чуть позже»"
                ),
                aiHint = "Быстрое дело на 1-3 минуты — сразу освободит оперативку головы.",
                categoryBadge = if (timeText != null || dateMillis != null) "📅 Календарь: ${timeText ?: "созвон"}" else "На пару минут • Мессенджер",
                estimatedMinutes = 2,
                shelf = "SIMPLE_STEPS",
                energyLevel = "FIVE_MIN",
                aiDraftType = "Черновик сообщения",
                aiDraftContent = "«Привет! Пишу по поводу $cleanPhrase. Всё в процессе, скоро пришлю детали.»",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText
            )
        }

        // 5. Полка: Простые шаги -> Легкое (EASY / Бытовое)
        val isBuy = lower.contains("купить") || lower.contains("заказать") || lower.contains("батарейк") || lower.contains("маркетплейс")
        return ParsedItemDto(
            originalQuote = original,
            title = cleanPhrase,
            simplifiedSteps = listOf(
                "Открыть поиск или приложение для $cleanPhrase",
                "Сделать одно простое действие на 30 секунд"
            ),
            aiHint = "Простое бытовое дело, не требующее умственного напряжения.",
            categoryBadge = if (timeText != null || dateMillis != null) "📦 Календарь • Доставка" else "Бытовое • Не требует фокуса",
            estimatedMinutes = 5,
            shelf = "SIMPLE_STEPS",
            energyLevel = "EASY",
            aiDraftType = if (isBuy) "Поисковый фильтр ИИ" else "Совет ИИ",
            aiDraftContent = if (isBuy) "Рекомендация: выбери проверенного продавца с рейтингом от 4.8 и быстрой доставкой."
            else "Начни с одного физического движения, чтобы запустить действие без сопротивления.",
            scheduledDateMillis = dateMillis,
            scheduledTimeText = timeText
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

