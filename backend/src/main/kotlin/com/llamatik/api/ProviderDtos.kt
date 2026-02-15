package com.llamatik.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// --- OpenAI DTOs ---

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float? = 1.0f,
    @SerialName("top_p") val topP: Float? = 1.0f,
    val n: Int? = 1,
    val stream: Boolean? = false,
    val stop: JsonElement? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val tools: List<Tool>? = null,
    @SerialName("tool_choice") val toolChoice: JsonElement? = null,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    val name: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
data class Tool(
    val type: String,
    val function: FunctionDefinition,
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null,
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: FunctionCall,
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
data class ResponseFormat(
    val type: String,
    @SerialName("json_schema") val jsonSchema: JsonElement? = null,
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: ChatUsage? = null,
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class ChatUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int,
)

// --- Ollama DTOs ---

@Serializable
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val system: String? = null,
    val template: String? = null,
    val context: List<Int>? = null,
    val stream: Boolean? = true,
    val raw: Boolean? = false,
    val format: String? = null,
    val options: Map<String, JsonElement>? = null,
)

@Serializable
data class OllamaGenerateResponse(
    val model: String,
    @SerialName("created_at") val createdAt: String,
    val response: String,
    val done: Boolean,
    val context: List<Int>? = null,
    @SerialName("total_duration") val totalDuration: Long? = null,
    @SerialName("load_duration") val loadDuration: Long? = null,
    @SerialName("prompt_eval_count") val promptEvalCount: Int? = null,
    @SerialName("eval_count") val evalCount: Int? = null,
)

@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean? = true,
    val format: String? = null,
    val tools: List<Tool>? = null,
    val options: Map<String, JsonElement>? = null,
)

@Serializable
data class OllamaChatResponse(
    val model: String,
    @SerialName("created_at") val createdAt: String,
    val message: ChatMessage,
    val done: Boolean,
    @SerialName("total_duration") val totalDuration: Long? = null,
    @SerialName("load_duration") val loadDuration: Long? = null,
    @SerialName("prompt_eval_count") val promptEvalCount: Int? = null,
    @SerialName("eval_count") val evalCount: Int? = null,
)
