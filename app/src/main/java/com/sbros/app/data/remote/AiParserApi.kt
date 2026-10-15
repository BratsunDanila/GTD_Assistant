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
                categoryBadge = "Идея • Отдых",
                estimatedMinutes = 0,
                shelf = "SOMEDAY",
                energyLevel = "EASY"
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
                categoryBadge = "Ожидание • Контроль",
                estimatedMinutes = 0,
                shelf = "WAITING",
                energyLevel = "EASY"
            )
        }

        // 3. Полка: Большие дела (BIG_THINGS)
        if (lower.contains("курсач") || lower.contains("курсов") || lower.contains("диплом") ||
            lower.contains("проект") || lower.contains("стать") || lower.contains("ремонт") ||
            lower.contains("переезд") || lower.contains("сдать")
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
                categoryBadge = "Большое дело: $projTitle",
                estimatedMinutes = 20,
                shelf = "BIG_THINGS",
                energyLevel = "FOCUS",
                matchedProjectId = matchedProj?.id,
                newProjectTitle = if (matchedProj == null) projTitle else null
            )
        }

        // 4. Полка: Простые шаги -> На 5 минут (FIVE_MIN)
        if (lower.contains("написать") || lower.contains("скинуть") || lower.contains("ответить") ||
            lower.contains("позвонить") || lower.contains("переслать") || lower.contains("сообщ") ||
            lower.contains("старост") || lower.contains("telegram") || lower.contains("чат")
        ) {
            return ParsedItemDto(
                originalQuote = original,
                title = cleanPhrase,
                simplifiedSteps = listOf(
                    "Открыть нужный диалог в мессенджере",
                    "Отправить короткое сообщение «Привет, скину чуть позже»"
                ),
                aiHint = "Быстрое дело на 1-3 минуты — сразу освободит оперативку головы.",
                categoryBadge = "На пару минут • Мессенджер",
                estimatedMinutes = 2,
                shelf = "SIMPLE_STEPS",
                energyLevel = "FIVE_MIN"
            )
        }

        // 5. Полка: Простые шаги -> Легкое (EASY / Бытовое)
        return ParsedItemDto(
            originalQuote = original,
            title = cleanPhrase,
            simplifiedSteps = listOf(
                "Открыть поиск или приложение для $cleanPhrase",
                "Сделать одно простое действие на 30 секунд"
            ),
            aiHint = "Простое бытовое дело, не требующее умственного напряжения.",
            categoryBadge = "Бытовое • Не требует фокуса",
            estimatedMinutes = 5,
            shelf = "SIMPLE_STEPS",
            energyLevel = "EASY"
        )
    }
}
