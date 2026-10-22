package com.sbros.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterChatRequest(
    val model: String? = null,
    val models: List<String>? = null,
    val messages: List<OpenRouterMessage>,
    @SerialName("response_format") val responseFormat: OpenRouterResponseFormat? = null,
    val temperature: Double = 0.2
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class OpenRouterChatResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice> = emptyList(),
    val error: OpenRouterErrorDto? = null
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class OpenRouterErrorDto(
    val message: String? = null,
    val code: Int? = null
)

@Serializable
data class AiDecompositionResponseDto(
    val steps: List<String> = emptyList()
)

@Serializable
data class AiDraftResultDto(
    @SerialName("draft_type") val draftType: String = "Черновик",
    @SerialName("draft_content") val draftContent: String = "",
    @SerialName("ai_advice") val aiAdvice: String = ""
)
