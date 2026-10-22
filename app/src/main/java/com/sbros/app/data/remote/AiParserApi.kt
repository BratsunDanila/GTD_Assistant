package com.sbros.app.data.remote

import android.util.Log
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.remote.dto.AiDraftResultDto
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

    suspend fun decomposeTask(
        taskTitle: String,
        category: String
    ): List<String>

    suspend fun generateDraft(
        taskTitle: String,
        category: String
    ): AiDraftResultDto?
}

@Singleton
class AiParserApiImpl : AiParserApi {

    private val openRouterClient: OpenRouterClient?
    private val preferencesManager: PreferencesManager?

    @Inject
    constructor(
        openRouterClient: OpenRouterClient,
        preferencesManager: PreferencesManager
    ) {
        this.openRouterClient = openRouterClient
        this.preferencesManager = preferencesManager
    }

    constructor() {
        this.openRouterClient = null
        this.preferencesManager = null
    }

    private val TAG = "AiParserApi"

    override suspend fun parseTranscript(
        rawTranscript: String,
        existingProjects: List<ProjectEntity>
    ): DumpResponseDto {
        val trimmed = rawTranscript.trim()
        if (trimmed.isBlank()) {
            return DumpResponseDto(emptyList())
        }

        if (openRouterClient != null) {
            val apiKey = preferencesManager?.getOpenRouterApiKey()
            val model = preferencesManager?.getOpenRouterModel().orEmpty()

            if (!apiKey.isNullOrBlank()) {
                Log.d(TAG, "Parsing transcript using OpenRouter AI model: $model")
                val projectTitles = existingProjects.map { it.title }
                try {
                    val aiResult = openRouterClient.parseTranscript(
                        rawTranscript = trimmed,
                        existingProjects = projectTitles,
                        apiKey = apiKey,
                        model = model
                    )

                    if (aiResult.isSuccess) {
                        val items = aiResult.getOrNull().orEmpty()
                        if (items.isNotEmpty()) {
                            Log.d(TAG, "OpenRouter successfully parsed ${items.size} tasks")
                            return DumpResponseDto(items)
                        } else {
                            Log.w(TAG, "OpenRouter returned empty items, falling back to offline parser")
                        }
                    } else {
                        val error = aiResult.exceptionOrNull()
                        Log.w(TAG, "OpenRouter call failed: ${error?.message}, falling back to offline parser")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "OpenRouter parsing error: ${e.message}, falling back to offline parser", e)
                }
            } else {
                Log.w(TAG, "OpenRouter API key is missing, using offline parser")
            }
        }

        // Офлайн умный GTD-парсер (фоллбэк при недоступности сети/таймауте/ошибках ИИ)
        Log.i(TAG, "Using smart offline GTD parser for transcript")
        return parseTranscriptOffline(trimmed, existingProjects)
    }

    override suspend fun decomposeTask(
        taskTitle: String,
        category: String
    ): List<String> {
        val apiKey = preferencesManager?.getOpenRouterApiKey()
        val model = preferencesManager?.getOpenRouterModel().orEmpty()

        if (!apiKey.isNullOrBlank() && openRouterClient != null) {
            val res = openRouterClient.decomposeTask(taskTitle, category, apiKey, model)
            if (res.isSuccess) {
                val steps = res.getOrNull().orEmpty()
                if (steps.isNotEmpty()) return steps.take(4)
            }
        }

        // Офлайн фоллбэк для декомпозиции (строго 3-4 емких шага)
        val t = taskTitle.lowercase(Locale.ROOT)
        return when {
            t.contains("аккумулятор") || t.contains("купить") || t.contains("заказать") -> listOf(
                "Открыть маркетплейс и найти «$taskTitle»",
                "Выбрать подходящий вариант по отзывам и цене",
                "Оформить заказ в ближайший пункт выдачи"
            )
            t.contains("старост") || t.contains("отправить") || t.contains("написать") || t.contains("сообщени") -> listOf(
                "Сформулировать текст сообщения",
                "Открыть диалог в мессенджере и отправить",
                "Дождаться ответа и зафиксировать итог"
            )
            t.contains("курсов") || t.contains("диплом") || t.contains("реферат") || t.contains("отчет") -> listOf(
                "Открыть документ с материалами",
                "Написать черновик текущего раздела без перфекционизма",
                "Сохранить и отправить на согласование"
            )
            else -> listOf(
                "Подготовить рабочее пространство для задачи «$taskTitle»",
                "Выполнить основной шаг за 15 минут без отвлечений",
                "Зафиксировать результат"
            )
        }.take(4)
    }

    override suspend fun generateDraft(
        taskTitle: String,
        category: String
    ): AiDraftResultDto? {
        val apiKey = preferencesManager?.getOpenRouterApiKey()
        val model = preferencesManager?.getOpenRouterModel().orEmpty()

        if (!apiKey.isNullOrBlank() && openRouterClient != null) {
            val res = openRouterClient.generateDraft(taskTitle, category, apiKey, model)
            if (res.isSuccess) {
                val draft = res.getOrNull()
                if (draft != null && draft.draftContent.isNotBlank()) {
                    return draft
                }
            }
        }

        // Высококачественный мгновенный офлайн-анализ (фоллбэк без воды и без усложнений)
        val t = taskTitle.lowercase(Locale.ROOT)
        return when {
            t.contains("позвонить") || t.contains("набрать") -> AiDraftResultDto(
                draftType = "Черновик звонка",
                draftContent = "«Здравствуйте! Звоню по поводу: $taskTitle. Удобно сейчас обсудить?»",
                aiAdvice = "Звонок займет меньше двух минут. Набери номер прямо сейчас."
            )
            t.contains("написать") || t.contains("отправить") || t.contains("старост") || t.contains("сообщени") || t.contains("уточнить") -> AiDraftResultDto(
                draftType = "Черновик сообщения",
                draftContent = "«Здравствуйте! Подскажите, пожалуйста, по вопросу: $taskTitle. Заранее спасибо!»",
                aiAdvice = "Скопируй текст заготовки и отправь в мессенджер."
            )
            t.contains("купить") || t.contains("заказать") -> AiDraftResultDto(
                draftType = "Поисковый запрос",
                draftContent = "Купить $taskTitle с быстрой доставкой",
                aiAdvice = "Вставь запрос в поиск маркетплейса и выбери первый подходящий вариант."
            )
            t.contains("курсов") || t.contains("диплом") || t.contains("отчет") || t.contains("реферат") -> AiDraftResultDto(
                draftType = "Первый шаг (2 минуты)",
                draftContent = "1. Открыть документ по теме «$taskTitle».\n2. Написать заголовок или тезис.\n3. Поставить таймер на 15 минут.",
                aiAdvice = "Не пытайся сделать всё сразу — начни с открытия документа."
            )
            else -> AiDraftResultDto(
                draftType = "Первый шаг (2 минуты)",
                draftContent = "Действие: $taskTitle.\nПодготовка: открыть рабочее окно и убрать отвлекающие факторы.",
                aiAdvice = "Сделай минимальное действие на 2 минуты, чтобы преодолеть сопротивление."
            )
        }
    }

    private fun parseTranscriptOffline(
        trimmed: String,
        existingProjects: List<ProjectEntity>
    ): DumpResponseDto {
        val items = mutableListOf<ParsedItemDto>()

        val sentences = trimmed.split(Regex("[,.;\n]+|\\s+и\\s+|\\s+еще\\s+|\\s+также\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (phrase in sentences) {
            val lower = phrase.lowercase(Locale.ROOT)
            if (isPureEmotion(lower)) continue

            val item = classifyPhrase(phrase, lower, existingProjects)
            if (item != null) items.add(item)
        }

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
            "как же все достало", "все бесит", "устал от всего", "жесть", "пиздец",
            "ужас", "нет сил", "не хочу ничего делать", "тяжело", "грустно", "лень",
            "капец как лень", "хочу спать", "блин", "как же я устал", "я устал", "устал",
            "ничего не успеваю", "капец"
        )
        val hasEmotion = emotionWords.any { lower.contains(it) }
        val hasAction = listOf("купить", "заказать", "написать", "позвонить", "сделать", "отправить", "курсач", "диплом", "глянуть", "посмотреть", "кино").any { lower.contains(it) }
        return hasEmotion && !hasAction
    }

    private fun classifyPhrase(
        original: String,
        lower: String,
        existingProjects: List<ProjectEntity>
    ): ParsedItemDto? {
        val daysRegex = Regex("(?:^|(?<=\\s))(в\\s+понедельник|во\\s+вторник|в\\s+среду|в\\s+четверг|в\\s+пятницу|в\\s+субботу|в\\s+воскресенье|сегодня|завтра|послезавтра)(?=\\s|$)", RegexOption.IGNORE_CASE)
        val timeRegex = Regex("(?:^|(?<=\\s))(?:в\\s+)?(?:[0-2]?[0-9][:.]00|[0-2]?[0-9]:[0-5][0-9]|[0-2]?[0-9]\\s+[0-5][0-9]|[0-2]?[0-9])(?:\\s*(?:часов|часа|ч))?(?:\\s+(?:вечера|дня|утра|ночи))?(?=\\s|$)", RegexOption.IGNORE_CASE)
        val fillersRegex = Regex("^(мне\\s+)?(надо|нужно|хочу|не\\s+забыть|стоит\\s+бы|было\\s+бы\\s+здорово|пора|обязательно)\\s+", RegexOption.IGNORE_CASE)
        val storeRegex = Regex("^(?:мне\\s+)?(?:нужно\\s+)?(?:сходить|пойти|съездить)\\s+в\\s+магазин\\s+(?:и\\s+)?", RegexOption.IGNORE_CASE)

        var cleanPhrase = original
        var prev: String
        do {
            prev = cleanPhrase
            cleanPhrase = cleanPhrase
                .replace(daysRegex, " ")
                .replace(timeRegex, " ")
                .trim()
                .replace(fillersRegex, "")
                .trim()
                .replace(storeRegex, "")
                .trim()
                .replace(Regex("\\s{2,}"), " ")
                .trim()
        } while (cleanPhrase != prev && cleanPhrase.isNotBlank())

        if (cleanPhrase.isBlank()) {
            cleanPhrase = original
                .replace(fillersRegex, "")
                .trim()
        }
        if (cleanPhrase.isBlank()) return null

        cleanPhrase = cleanPhrase.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val timeText = extractTime(lower)
        val dateMillis = extractDateMillis(lower)

        val contextCategory = when {
            lower.contains("олимпиад") || lower.contains("хакатон") || lower.contains("контест") ||
                    lower.contains("курсов") || lower.contains("диплом") || lower.contains("вуз") ||
                    lower.contains("сесси") || lower.contains("экзамен") || lower.contains("препод") ||
                    lower.contains("универ") || lower.contains("лекци") || lower.contains("декан") -> "Учеба и вуз"

            lower.contains("купить") || lower.contains("заказать") || lower.contains("озон") ||
                    lower.contains("вб") || lower.contains("wb") || lower.contains("аптек") ||
                    lower.contains("магазин") || lower.contains("аккумулятор") -> "Покупки и заказы"

            lower.contains("код") || lower.contains("room") || lower.contains("баг") ||
                    lower.contains("фич") || lower.contains("проект") || lower.contains("git") ||
                    lower.contains("коммит") || lower.contains("сервер") || lower.contains("api") -> "Разработка и код"

            else -> "Дом и быт"
        }

        val difficulty = when {
            lower.contains("олимпиад") || lower.contains("хакатон") || lower.contains("контест") ||
                    lower.contains("диплом") || lower.contains("курсов") || lower.contains("курсач") ||
                    lower.contains("проект") || lower.contains("экзамен") || lower.contains("сесси") ||
                    lower.contains("ремонт") || lower.contains("отчет") -> "HARD"

            lower.contains("сдать") || lower.contains("позвонить") || lower.contains("написать") ||
                    lower.contains("составить") || lower.contains("выбрать") || lower.contains("отправить") ||
                    lower.contains("настроить") || lower.contains("уборк") || lower.contains("старост") ||
                    lower.contains("зачет") -> "MEDIUM"

            else -> "EASY"
        }

        val energyLevel = when (difficulty) {
            "HARD" -> "FLOW"
            "MEDIUM" -> "SINGLE_STEP"
            else -> "MECHANICAL"
        }

        val suggestedSteps = when (contextCategory) {
            "Учеба и вуз" -> listOf(
                "Открыть методичку и прочитать требования к разделу",
                "Написать черновик на 1-2 страницы без самокритики",
                "Проверить оформление и отправить на согласование"
            )
            "Покупки и заказы" -> listOf(
                "Открыть маркетплейс и найти товар",
                "Выбрать подходящий по отзывам и цене",
                "Оформить заказ в ближайший пункт выдачи"
            )
            "Разработка и код" -> listOf(
                "Локализовать проблему и воспроизвести сценарий",
                "Написать минимальный фикс и запустить тесты",
                "Сделать коммит и запушить ветку"
            )
            else -> listOf(
                "Подготовить все необходимое для начала",
                "Поставить таймер на 15 минут и начать делать",
                "Зафиксировать результат"
            )
        }

        val aiAdvice = when (difficulty) {
            "HARD" -> "Задача кажется объемной. Главное — сесть и сделать первый шаг на 5 минут."
            "MEDIUM" -> "Сделай прямо сейчас, чтобы освободить мысли для важного."
            else -> "Займет пару минут. Сделай и вычеркни с чистой совестью."
        }

        // Проверка на "В ожидании" (WAITING)
        if (lower.contains("жду") || lower.contains("ждем") || lower.contains("ответит") ||
            lower.contains("пришлет") || lower.contains("доставка") || lower.contains("курьер")
        ) {
            return ParsedItemDto(
                originalQuote = "Из мыслей: «$original»",
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Написать и вежливо уточнить статус",
                    "Поставить напоминание на вечер"
                ),
                aiHint = "Ты делегировал или ждешь. Зафиксировано на полке ожидания.",
                categoryBadge = "В ожидании ответа",
                estimatedMinutes = 2,
                shelf = "WAITING",
                energyLevel = "MECHANICAL",
                difficulty = "EASY",
                aiDraftType = "Уточнение статуса",
                aiDraftContent = "Здравствуйте! Подскажите, пожалуйста, удалось ли посмотреть?",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText,
                contextTag = "phone",
                contextCategory = contextCategory,
                aiSuggestedSteps = emptyList(), // Не разбиваем при первичном сбросе мыслей
                aiAdvice = "Не держи это в голове — когда ответят, задача вернется в фокус."
            )
        }

        // Проверка на "Когда-нибудь" (SOMEDAY)
        if (lower.contains("когда-нибудь") || lower.contains("в будущем") || lower.contains("мечтаю") ||
            lower.contains("хотелось бы когда-то") || lower.contains("посмотреть фильм") ||
            lower.contains("прочитать книгу") || lower.contains("кино") || lower.contains("фильм") ||
            lower.contains("сериал")
        ) {
            return ParsedItemDto(
                originalQuote = "Из мыслей: «$original»",
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Сохранить ссылку в закладки",
                    "Выделить 20 минут в выходной"
                ),
                aiHint = "Отличная идея на будущее. Сохранена на полке 'Когда-нибудь'.",
                categoryBadge = "Идея на будущее",
                estimatedMinutes = 15,
                shelf = "SOMEDAY",
                energyLevel = "FLOW",
                difficulty = "EASY",
                aiDraftType = "Заметка",
                aiDraftContent = "Название: $cleanPhrase\nЗаметка: изучить в спокойный вечер.",
                scheduledDateMillis = null,
                scheduledTimeText = null,
                contextTag = "home",
                contextCategory = contextCategory,
                aiSuggestedSteps = emptyList(),
                aiAdvice = "Такие идеи важно сохранять, чтобы не потерять вдохновение."
            )
        }

        // Проверка на "Большое дело" (BIG_THINGS)
        val isBigThing = difficulty == "HARD" ||
                lower.contains("проект") || lower.contains("диплом") || lower.contains("курсов") ||
                lower.contains("курсач") || lower.contains("олимпиад") || lower.contains("хакатон") ||
                lower.contains("ремонт") || lower.contains("переезд")

        if (isBigThing) {
            val projTitle = when {
                lower.contains("олимпиад") -> "Олимпиада"
                lower.contains("хакатон") -> "Хакатон"
                lower.contains("диплом") -> "Дипломная работа"
                lower.contains("курсов") || lower.contains("курсач") -> "Курсовая работа"
                lower.contains("ремонт") -> "Ремонт"
                else -> cleanPhrase
            }

            val matchedProj = existingProjects.find { it.title.equals(projTitle, ignoreCase = true) }

            return ParsedItemDto(
                originalQuote = "Из мыслей: «$original»",
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Открыть документ и написать 3 тезиса",
                    "Выделить ровно 20 минут без телефона"
                ),
                aiHint = "Большое дело разбито на первый понятный шаг.",
                categoryBadge = when {
                    timeText != null -> "Дедлайн: $timeText"
                    dateMillis != null -> "$contextCategory • Проект"
                    else -> "$contextCategory • Проект"
                },
                estimatedMinutes = 20,
                shelf = "BIG_THINGS",
                energyLevel = energyLevel,
                difficulty = difficulty,
                matchedProjectId = matchedProj?.id,
                newProjectTitle = if (matchedProj == null) projTitle else null,
                aiDraftType = "Стартовый план",
                aiDraftContent = "1. Сформулировать 3 ключевых вопроса.\n2. Найти 2 источника.\n3. Набросать черновой план.",
                scheduledDateMillis = dateMillis,
                scheduledTimeText = timeText,
                contextTag = "pc",
                contextCategory = contextCategory,
                aiSuggestedSteps = emptyList(), // Не разбиваем при первичном сбросе мыслей
                aiAdvice = aiAdvice
            )
        }

        // Обычное быстрое дело (SIMPLE_STEPS)
        val isBuy = contextCategory == "Покупки и заказы"
        return ParsedItemDto(
            originalQuote = "Из мыслей: «$original»",
            title = cleanPhrase,
            simplifiedSteps = listOf(
                "Сделать сразу за 2 минуты",
                "Поставить таймер на 10 минут"
            ),
            aiHint = "Конкретное действие, можно сделать прямо сейчас.",
            categoryBadge = when {
                timeText != null -> "Время: $timeText"
                dateMillis != null -> "$contextCategory • В календаре"
                else -> "$contextCategory • ${if (difficulty == "EASY") "Легкое" else "Быстрое"}"
            },
            estimatedMinutes = if (isBuy) 3 else 5,
            shelf = "SIMPLE_STEPS",
            energyLevel = energyLevel,
            difficulty = difficulty,
            aiDraftType = if (isBuy) "Поисковый запрос" else "Черновик сообщения",
            aiDraftContent = if (isBuy) "Запрос: купить $cleanPhrase с хорошими отзывами."
            else "Привет! Насчет $cleanPhrase: сможем обсудить?",
            scheduledDateMillis = dateMillis,
            scheduledTimeText = timeText,
            contextTag = if (isBuy) "out" else if (lower.contains("позвонить") || lower.contains("написать")) "phone" else "home",
            contextCategory = contextCategory,
            aiSuggestedSteps = emptyList(), // Не разбиваем при первичном сбросе мыслей
            aiAdvice = aiAdvice
        )
    }

    private fun extractTime(lower: String): String? {
        // 1. Формат "в 9:45", "в 6:00", "18:30"
        val colonRegex = Regex("(?:^|(?<=\\s))(?:в\\s+)?([0-2]?[0-9])[:.]([0-5][0-9])(?=\\s|$)")
        val colonMatch = colonRegex.find(lower)
        if (colonMatch != null) {
            var hour = colonMatch.groupValues[1].toIntOrNull() ?: return null
            val min = colonMatch.groupValues[2].toIntOrNull() ?: 0
            if (lower.contains("вечер") && hour in 1..11) {
                hour += 12
            } else if (lower.contains("дня") && hour in 1..5) {
                hour += 12
            } else if (lower.contains("ночи") && hour == 12) {
                hour = 0
            }
            return String.format(Locale.ROOT, "%02d:%02d", hour, min)
        }

        // 2. Формат "в 9 45", "в 09 45"
        val spaceTimeRegex = Regex("(?:^|(?<=\\s))в\\s+([0-2]?[0-9])\\s+([0-5][0-9])(?=\\s|$)")
        val spaceMatch = spaceTimeRegex.find(lower)
        if (spaceMatch != null) {
            var hour = spaceMatch.groupValues[1].toIntOrNull() ?: return null
            val min = spaceMatch.groupValues[2].toIntOrNull() ?: 0
            if (lower.contains("вечер") && hour in 1..11) {
                hour += 12
            } else if (lower.contains("дня") && hour in 1..5) {
                hour += 12
            }
            return String.format(Locale.ROOT, "%02d:%02d", hour, min)
        }

        // 3. Формат "в 6 вечера", "в 7 вечера", "в 2 дня"
        val wordTimeRegex = Regex("(?:^|(?<=\\s))в\\s+([0-2]?[0-9])(?:\\s*(?:часов|часа|ч))?(?:\\s+(вечера|дня|утра|ночи))(?=\\s|$)")
        val wordMatch = wordTimeRegex.find(lower)
        if (wordMatch != null) {
            var hour = wordMatch.groupValues[1].toIntOrNull() ?: return null
            val period = wordMatch.groupValues[2]
            if (period.contains("вечер") && hour in 1..11) {
                hour += 12
            } else if (period.contains("дня") && hour in 1..5) {
                hour += 12
            } else if (period.contains("ночи") && hour == 12) {
                hour = 0
            }
            return String.format(Locale.ROOT, "%02d:00", hour)
        }

        return null
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
            else -> {
                val targetDayOfWeek = when {
                    lower.contains("понедельник") -> java.util.Calendar.MONDAY
                    lower.contains("вторник") -> java.util.Calendar.TUESDAY
                    lower.contains("среду") || lower.contains("среда") -> java.util.Calendar.WEDNESDAY
                    lower.contains("четверг") -> java.util.Calendar.THURSDAY
                    lower.contains("пятниц") -> java.util.Calendar.FRIDAY
                    lower.contains("суббот") -> java.util.Calendar.SATURDAY
                    lower.contains("воскресень") -> java.util.Calendar.SUNDAY
                    else -> null
                }
                if (targetDayOfWeek != null) {
                    val currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK)
                    var diff = targetDayOfWeek - currentDay
                    if (diff <= 0) diff += 7
                    cal.add(java.util.Calendar.DAY_OF_YEAR, diff)
                    return cal.timeInMillis
                }
            }
        }
        return null
    }
}
