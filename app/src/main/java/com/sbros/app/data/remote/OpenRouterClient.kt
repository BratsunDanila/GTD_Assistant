package com.sbros.app.data.remote

import android.util.Log
import com.sbros.app.data.remote.dto.AiDecompositionResponseDto
import com.sbros.app.data.remote.dto.AiDraftResultDto
import com.sbros.app.data.remote.dto.DumpResponseDto
import com.sbros.app.data.remote.dto.OpenRouterChatRequest
import com.sbros.app.data.remote.dto.OpenRouterChatResponse
import com.sbros.app.data.remote.dto.OpenRouterMessage
import com.sbros.app.data.remote.dto.OpenRouterResponseFormat
import com.sbros.app.data.remote.dto.ParsedItemDto
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterClient @Inject constructor() {

    private val TAG = "OpenRouterClient"
    private val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        val FALLBACK_MODELS = listOf(
            "recursive/flint-v1.0:free",
            "dots-studio/dots-3-note-preview:free",
            "nvidia/nemotron-3.5-super-vl:free"
        )
    }

    private fun getModelList(requestedModel: String): List<String> {
        val list = mutableListOf<String>()
        if (requestedModel.isNotBlank()) {
            list.add(requestedModel.trim())
        }
        list.addAll(FALLBACK_MODELS)
        return list.distinct().take(3)
    }

    private fun buildChatRequest(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double
    ): OpenRouterChatRequest {
        val isTokenHarbor = apiKey.trim().startsWith("thk_")
        return if (isTokenHarbor) {
            val effectiveModel = if (model.isNotBlank()) model.trim() else "deepseek-v4.1-flash:free"
            OpenRouterChatRequest(
                model = effectiveModel,
                models = null,
                messages = listOf(
                    OpenRouterMessage(role = "system", content = systemPrompt),
                    OpenRouterMessage(role = "user", content = userPrompt)
                ),
                responseFormat = OpenRouterResponseFormat(type = "json_object"),
                temperature = temperature
            )
        } else {
            val modelList = getModelList(model)
            OpenRouterChatRequest(
                model = null,
                models = modelList,
                messages = listOf(
                    OpenRouterMessage(role = "system", content = systemPrompt),
                    OpenRouterMessage(role = "user", content = userPrompt)
                ),
                responseFormat = OpenRouterResponseFormat(type = "json_object"),
                temperature = temperature
            )
        }
    }

    suspend fun parseTranscript(
        rawTranscript: String,
        existingProjects: List<String>,
        apiKey: String,
        model: String
    ): Result<List<ParsedItemDto>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenRouter API key is missing"))
        }

        val currentDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm (EEEE)", Locale("ru")).format(Date())
        val projectsStr = if (existingProjects.isNotEmpty()) {
            existingProjects.joinToString(", ")
        } else {
            "нет"
        }

        val systemPrompt = """
            Ты — быстрый и умный GTD-парсер задач в приложении "Сброс".
            Твоя задача — преобразовать поток мыслей или диктовку пользователя в компактный список конкретных задач БЕЗ МУСОРА.

            Контекст:
            - Текущее время и день: $currentDateStr
            - Существующие проекты пользователя: $projectsStr

            СТРОГИЕ ПРАВИЛА:
            1. ЧИСТОЕ НАЗВАНИЕ (title):
               - Конкретное действие на русском языке с глагола в инфинитиве (например: "Купить чебуреки", "Сдать курсовую работу", "Позвонить старосте").
               - В ТЕКСТЕ НАЗВАНИЯ (title) НЕ ДОЛЖНО БЫТЬ ВРЕМЕНИ И ДАТ! Запрещено включать в title слова "сегодня", "завтра", "в 9:45", "в 15:00", дни недели. Вся информация о времени выносится ТОЛЬКО в поля scheduled_time_text и scheduled_date_millis для календаря!
               - Убирай весь мусор, эмоции и вводные конструкции ("сегодня мне нужно сходить в магазин", "надо", "не забыть бы").
            2. ВРЕМЯ И КАЛЕНДАРЬ (НИКОГДА НЕ ВЫДУМЫВАЙ ВРЕМЯ! СТРОГО 24-ЧАСОВОЙ ФОРМАТ):
               - scheduled_time_text: точное время цифрами строго в 24-часовом формате "HH:mm" (например: "09:45", "18:00", "21:30") ТОЛЬКО ЕСЛИ пользователь ЯВНО назвал конкретные часы/минуты. Если пользователь НЕ назвал конкретные часы/минуты — СТРОГО null! НИ В КОЕМ СЛУЧАЕ НЕ ПРИДУМЫВАЙ ВРЕМЯ САМ!
               - ОБЯЗАТЕЛЬНАЯ КОНВЕРТАЦИЯ ВРЕМЕНИ СУТОК В 24-ЧАСОВОЙ ФОРМАТ:
                 * Если сказано "вечера" или "в X:00 вечера": "в 6 вечера" / "в 6:00 вечера" -> "18:00", "в 7 вечера" -> "19:00", "в 8 вечера" -> "20:00", "в 9 вечера" -> "21:00", "в 10 вечера" -> "22:00", "в 11 вечера" -> "23:00". НИ В КОЕМ СЛУЧАЕ НЕ СТАВЬ 06:00, ЕСЛИ ЭТО ВЕЧЕР!
                 * Если сказано "дня": "в 1 час дня" -> "13:00", "в 2 дня" -> "14:00", "в 3 дня" -> "15:00", "в 4 дня" -> "16:00", "в 5 дня" -> "17:00".
                 * Если сказано "ночи": "в 12 ночи" -> "00:00", "в 1 ночи" -> "01:00".
               - scheduled_date_millis: timestamp даты в миллисекундах (00:00 названного дня). Если пользователь назвал день (например, "сегодня", "завтра", "послезавтра", "в пятницу", "в понедельник"), но НЕ назвал время — scheduled_date_millis заполняется этой датой (00:00), а scheduled_time_text СТРОГО null!
               - Если день/дата вообще не упоминались — scheduled_date_millis: null.
            3. НЕ РАЗБИВАЙ НА ШАГИ (ВАЖНО ДЛЯ СКОРОСТИ!):
               - НЕ разбивай задачи на подшаги! Пользователь разобьет задачу позже отдельной кнопкой на карточке.
               - Поля ai_suggested_steps и simplified_steps ВСЕГДА должны быть пустыми массивами [].
            4. ПОЛКА (shelf): "SIMPLE_STEPS" (быстрое действие), "BIG_THINGS" (курсовая, диплом, проект, олимпиада), "WAITING" (ожидание), "SOMEDAY" (на потом/отдых), "REFERENCE" (заметка).
            5. КАТЕГОРИЯ (context_category): "Дом и быт", "Учеба и вуз", "Разработка и код", "Покупки и заказы".
            6. СЛОЖНОСТЬ (difficulty) — ОБЯЗАТЕЛЬНО ОБЪЕКТИВНО РАЗДЕЛЯЙ ЗАДАЧИ, НЕ СТАВЬ ВСЁ В "EASY":
               - "EASY" (Просто): мелкие, механические или быстрые действия до 10-15 минут без умственного напряжения (купить чебуреки/хлеб/продукты, вынести мусор, помыть посуду, забрать заказ с маркетплейса, оплатить интернет, полить цветы, погулять с девушкой/другом).
               - "MEDIUM" (Средне): действия требующие коммуникации, концентрации или 15-45 минут (позвонить старосте/преподавателю/врачу, написать и отправить важное сообщение/заявление, сдать курсовую работу/лабораторную, настроить технику/роутер, сделать уборку в комнате, выбрать товар/подарок, сходить в банк).
               - "HARD" (Сложно): масштабные задачи, проекты, олимпиады, написание объемных текстов, подготовка к экзаменам от 1 часа и более (написать курсовую работу/диплом/статью, олимпиада, хакатон, контест, соревнование, подготовиться к экзамену/сессии, разработать фичу/код/сервер, спланировать проект или поездку, генеральная уборка/ремонт).
               - ПРАВИЛО ДОМИНАНТЫ ОБЪЕКТА НАД ГЛАГОЛОМ:
                 Если предмет задачи сложный (олимпиада, диплом, курсовая, экзамен, проект, хакатон), сложность СТРОГО "HARD", даже если пользователь использовал вводные глаголы вроде "начать писать", "сесть за...", "попробовать решить", "поделать". "Начать писать олимпиаду" — это СТРОГО "HARD", потому что сама олимпиада — это сложное и ответственное испытание!

            ОТВЕТ ДОЛЖЕН БЫТЬ СТРОГО В ФОРМАТЕ JSON:
            {
              "parsed_items": [
                {
                  "original_quote": "исходная фраза",
                  "title": "Купить чебуреки в магазине",
                  "shelf": "SIMPLE_STEPS",
                  "difficulty": "EASY",
                  "energy_level": "EASY",
                  "estimated_minutes": 10,
                  "category_badge": "Покупки и заказы",
                  "context_category": "Покупки и заказы",
                  "context_tag": "out",
                  "ai_hint": "Быстрая покупка.",
                  "scheduled_time_text": null,
                  "scheduled_date_millis": null,
                  "matched_project_id": null,
                  "new_project_title": null,
                  "ai_suggested_steps": [],
                  "simplified_steps": []
                }
              ]
            }
        """.trimIndent()

        try {
            withTimeout(45_000L) {
                val requestBody = buildChatRequest(
                    apiKey = apiKey,
                    model = model,
                    systemPrompt = systemPrompt,
                    userPrompt = rawTranscript,
                    temperature = 0.2
                )

                val rawJson = executeOpenRouterCall(requestBody, apiKey)
                val cleaned = cleanJsonOutput(rawJson)
                val items = try {
                    if (cleaned.startsWith("[")) {
                        json.decodeFromString<List<ParsedItemDto>>(cleaned)
                    } else {
                        val parsedResponse = json.decodeFromString<DumpResponseDto>(cleaned)
                        if (parsedResponse.parsedItems.isNotEmpty()) {
                            parsedResponse.parsedItems
                        } else {
                            val jsonElement = json.parseToJsonElement(cleaned)
                            val jsonObject = jsonElement.jsonObject
                            val array = jsonObject["parsed_items"]?.jsonArray
                                ?: jsonObject["tasks"]?.jsonArray
                                ?: jsonObject["items"]?.jsonArray
                            array?.mapNotNull { el ->
                                try { json.decodeFromJsonElement<ParsedItemDto>(el) } catch (_: Exception) { null }
                            }.orEmpty()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed parsing JSON from OpenRouter output: $cleaned", e)
                    emptyList()
                }

                val sanitizedItems = items.map { sanitizeParsedItem(it) }
                if (sanitizedItems.isNotEmpty()) {
                    Result.success(sanitizedItems)
                } else {
                    Result.failure(IllegalStateException("ИИ вернул пустой разбор задач"))
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "OpenRouter timeout exceeded 45s")
            Result.failure(IllegalStateException("Модель перегружена: время ожидания превысило 45 секунд."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "OpenRouter socket timeout: ${e.message}")
            Result.failure(IllegalStateException("Превышено время ожидания ответа от OpenRouter (сервер перегружен)."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing transcript via OpenRouter: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractTimeFromText(text: String): String? {
        val lower = text.lowercase(Locale.ROOT)

        // 1. Формат "в 9:45", "в 6:00", "18:30"
        val colonRegex = Regex("(?:^|(?<=\\s))(?:в\\s+)?([0-2]?[0-9])[:.]([0-5][0-9])(?=\\s|$)")
        val colonMatch = colonRegex.find(lower)
        if (colonMatch != null) {
            var hour = colonMatch.groupValues[1].toIntOrNull() ?: 0
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
            var hour = spaceMatch.groupValues[1].toIntOrNull() ?: 0
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
            var hour = wordMatch.groupValues[1].toIntOrNull() ?: 0
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

    private fun sanitizeParsedItem(item: ParsedItemDto): ParsedItemDto {
        val daysRegex = Regex("(?:^|(?<=\\s))(в\\s+понедельник|во\\s+вторник|в\\s+среду|в\\s+четверг|в\\s+пятницу|в\\s+субботу|в\\s+воскресенье|сегодня|завтра|послезавтра)(?=\\s|$)", RegexOption.IGNORE_CASE)
        val timeRegex = Regex("(?:^|(?<=\\s))(?:в\\s+)?(?:[0-2]?[0-9][:.]00|[0-2]?[0-9]:[0-5][0-9]|[0-2]?[0-9]\\s+[0-5][0-9]|[0-2]?[0-9])(?:\\s*(?:часов|часа|ч))?(?:\\s+(?:вечера|дня|утра|ночи))?(?=\\s|$)", RegexOption.IGNORE_CASE)
        val fillersRegex = Regex("^(мне\\s+)?(надо|нужно|хочу|не\\s+забыть|стоит\\s+бы|было\\s+бы\\s+здорово|пора|обязательно)\\s+", RegexOption.IGNORE_CASE)
        val storeRegex = Regex("^(?:мне\\s+)?(?:нужно\\s+)?(?:сходить|пойти|съездить)\\s+в\\s+магазин\\s+(?:и\\s+)?", RegexOption.IGNORE_CASE)

        val rawTitle = item.title.ifBlank { item.originalQuote }
        var cleanTitle = rawTitle
        var prev: String
        do {
            prev = cleanTitle
            cleanTitle = cleanTitle
                .replace(daysRegex, " ")
                .replace(timeRegex, " ")
                .trim()
                .replace(fillersRegex, "")
                .trim()
                .replace(storeRegex, "")
                .trim()
                .replace(Regex("\\s{2,}"), " ")
                .trim()
        } while (cleanTitle != prev && cleanTitle.isNotBlank())

        if (cleanTitle.isBlank()) {
            cleanTitle = rawTitle.trim()
        }
        cleanTitle = cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        // ПРАВИЛО 1: ИИ НЕ ДОЛЖЕН ВЫДУМЫВАТЬ ВРЕМЯ + 24-ЧАСОВАЯ КОНВЕРТАЦИЯ ВЕЧЕРА/ДНЯ
        val sourceText = rawTitle + " " + item.originalQuote
        val extractedTime = extractTimeFromText(sourceText)
        val finalTimeText = extractedTime ?: item.scheduledTimeText?.let { t ->
            val lower = sourceText.lowercase(Locale.ROOT)
            val parts = t.split(":")
            if (parts.size == 2) {
                var h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                if (lower.contains("вечер") && h in 1..11) h += 12
                else if (lower.contains("дня") && h in 1..5) h += 12
                else if (lower.contains("ночи") && h == 12) h = 0
                String.format(Locale.ROOT, "%02d:%02d", h, m)
            } else t
        }

        // ПРАВИЛО 2: ДЕНЬ БЕЗ ВРЕМЕНИ ДЛЯ КАЛЕНДАРЯ
        var finalDateMillis = item.scheduledDateMillis
        if (finalDateMillis == null) {
            val lower = sourceText.lowercase(Locale.ROOT)
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)

            when {
                lower.contains("сегодня") -> finalDateMillis = cal.timeInMillis
                lower.contains("завтра") -> {
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    finalDateMillis = cal.timeInMillis
                }
                lower.contains("послезавтра") -> {
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
                    finalDateMillis = cal.timeInMillis
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
                        finalDateMillis = cal.timeInMillis
                    }
                }
            }
        }

        // ПРАВИЛО 3: СЛОЖНОСТЬ — ДОМИНАНТА ОБЪЕКТА НАД ВВОДНЫМИ ГЛАГОЛАМИ
        val lowerSource = sourceText.lowercase(Locale.ROOT)
        val finalDifficulty = when {
            lowerSource.contains("олимпиад") || lowerSource.contains("хакатон") || lowerSource.contains("контест") ||
            lowerSource.contains("диплом") || lowerSource.contains("курсов") || lowerSource.contains("курсач") ||
            lowerSource.contains("экзамен") || lowerSource.contains("сесси") -> "HARD"
            else -> item.difficulty.ifBlank { "EASY" }
        }

        val finalShelf = if (finalDifficulty == "HARD" && item.shelf == "SIMPLE_STEPS") {
            "BIG_THINGS"
        } else {
            item.shelf
        }

        val finalContextCategory = if (lowerSource.contains("олимпиад") && item.contextCategory.isBlank()) {
            "Учеба и вуз"
        } else {
            item.contextCategory.ifBlank { "Дом и быт" }
        }

        return item.copy(
            title = cleanTitle,
            shelf = finalShelf,
            difficulty = finalDifficulty,
            contextCategory = finalContextCategory,
            scheduledTimeText = finalTimeText,
            scheduledDateMillis = finalDateMillis,
            aiSuggestedSteps = emptyList(), // Не разбиваем при сбросе мыслей
            simplifiedSteps = emptyList()   // Не разбиваем при сбросе мыслей
        )
    }

    suspend fun decomposeTask(
        taskTitle: String,
        category: String,
        apiKey: String,
        model: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenRouter API key is missing"))
        }

        val prompt = """
            Разбей задачу "$taskTitle" (категория: $category) максимум на 3-4 конкретных, последовательных, простых шага (строго от 2 до 4 шагов, ни в коем случае не более 4!).
            Каждый шаг должен быть коротким физическим действием на русском языке, начинающимся с глагола в инфинитиве (например: "Открыть...", "Найти...", "Составить...").
            Не раздувай задачу, не усложняй работу, пиши предельно кратко и по делу. Без эмодзи.

            Ответь ТОЛЬКО JSON в формате:
            {
              "steps": [
                "Шаг 1",
                "Шаг 2",
                "Шаг 3"
              ]
            }
        """.trimIndent()

        try {
            val requestBody = buildChatRequest(
                apiKey = apiKey,
                model = model,
                systemPrompt = "Ты — эксперт по декомпозиции задач в системе GTD. Держи декомпозицию компактной: не более 3-4 шагов.",
                userPrompt = prompt,
                temperature = 0.2
            )

            val rawJson = executeOpenRouterCall(requestBody, apiKey)
            val responseDto = json.decodeFromString<AiDecompositionResponseDto>(cleanJsonOutput(rawJson))
            val validSteps = responseDto.steps.filter { it.isNotBlank() }.take(4)
            if (validSteps.isNotEmpty()) {
                Result.success(validSteps)
            } else {
                Result.failure(IllegalStateException("Empty steps returned from model"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed decomposing task via OpenRouter: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateDraft(
        taskTitle: String,
        category: String,
        apiKey: String,
        model: String
    ): Result<AiDraftResultDto> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OpenRouter API key is missing"))
        }

        val prompt = """
            Для задачи "$taskTitle" (категория: $category) составь конкретный и практичный анализ без воды и усложнений:
            1. Тип заготовки (draft_type): например, "Черновик сообщения", "Поисковый запрос", "Первый шаг (2 мин)".
            2. Готовый текст (draft_content): конкретный короткий текст, который пользователь может скопировать и сразу отправить или вставить в поиск, либо конкретное действие на первые 2 минуты.
            3. Краткий совет (ai_advice): 1 простое предложение, помогающее начать прямо сейчас без сопротивления.
            Без эмодзи, на русском языке, не раздувай задачу и не усложняй работу.

            Ответь ТОЛЬКО JSON в формате:
            {
              "draft_type": "Черновик сообщения",
              "draft_content": "Здравствуйте! ...",
              "ai_advice": "Отправьте это сообщение прямо сейчас, это займет менее минуты."
            }
        """.trimIndent()

        try {
            val requestBody = buildChatRequest(
                apiKey = apiKey,
                model = model,
                systemPrompt = "Ты — умный ассистент приложения 'Сброс'.",
                userPrompt = prompt,
                temperature = 0.3
            )

            val rawJson = executeOpenRouterCall(requestBody, apiKey)
            val draft = json.decodeFromString<AiDraftResultDto>(cleanJsonOutput(rawJson))
            Result.success(draft)
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating draft via OpenRouter: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testApiKey(apiKey: String, model: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val effectiveModel = if (model.isNotBlank()) model else if (apiKey.startsWith("thk_")) "deepseek-v4.1-flash:free" else "recursive/flint-v1.0:free"
            val requestBody = OpenRouterChatRequest(
                model = effectiveModel,
                messages = listOf(
                    OpenRouterMessage(role = "user", content = "Ping")
                ),
                temperature = 0.0
            )
            executeOpenRouterCall(requestBody, apiKey)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeOpenRouterCall(chatRequest: OpenRouterChatRequest, apiKey: String): String {
        val jsonPayload = json.encodeToString(chatRequest)
        val isTokenHarbor = apiKey.trim().startsWith("thk_")
        val targetUrl = if (isTokenHarbor) "https://tokenharbor.ai/v1/chat/completions" else BASE_URL

        Log.d(TAG, "Executing LLM HTTP call to $targetUrl: $jsonPayload")
        val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .post(body)

        if (!isTokenHarbor) {
            requestBuilder.addHeader("HTTP-Referer", "https://sbros.app")
            requestBuilder.addHeader("X-Title", "Sbros GTD")
        }

        val request = requestBuilder.build()

        val response = httpClient.newCall(request).await()
        response.use { resp ->
            val responseString = resp.body?.string().orEmpty()
            Log.d(TAG, "LLM HTTP response [HTTP ${resp.code}]: $responseString")
            if (!resp.isSuccessful) {
                val errorMsg = when {
                    responseString.contains("email_verification_required", ignoreCase = true) ->
                        "Требуется подтвердить email на tokenharbor.ai. Откройте ссылку из письма на почте."
                    resp.code == 403 || responseString.contains("Access denied by security policy", ignoreCase = true) ->
                        "Доступ заблокирован (HTTP 403: Access denied). Требуется включение VPN."
                    resp.code == 429 ->
                        "Превышен лимит запросов к модели (HTTP 429). Попробуйте позже."
                    else -> try {
                        val errDto = json.decodeFromString<OpenRouterChatResponse>(responseString)
                        errDto.error?.message ?: "Ошибка ИИ [HTTP ${resp.code}]"
                    } catch (_: Exception) {
                        "Ошибка ИИ [HTTP ${resp.code}]: $responseString"
                    }
                }
                throw IllegalStateException(errorMsg)
            }

            val chatResponse = json.decodeFromString<OpenRouterChatResponse>(responseString)
            val content = chatResponse.choices.firstOrNull()?.message?.content
            if (content.isNullOrBlank()) {
                throw IllegalStateException("Empty content received from LLM")
            }
            return content
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (_: Throwable) {}
        }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }

    private fun cleanJsonOutput(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```JSON")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        cleaned = cleaned.trim()

        val firstBrace = cleaned.indexOf('{')
        val firstBracket = cleaned.indexOf('[')

        if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            val lastBracket = cleaned.lastIndexOf(']')
            if (lastBracket > firstBracket) {
                cleaned = cleaned.substring(firstBracket, lastBracket + 1)
            }
        } else if (firstBrace != -1) {
            val lastBrace = cleaned.lastIndexOf('}')
            if (lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1)
            }
        }
        return cleaned.trim()
    }
}
