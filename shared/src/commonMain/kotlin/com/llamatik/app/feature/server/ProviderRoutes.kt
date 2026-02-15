package com.llamatik.app.feature.server

import com.llamatik.api.ChatChoice
import com.llamatik.api.ChatCompletionRequest
import com.llamatik.api.ChatCompletionResponse
import com.llamatik.api.ChatMessage
import com.llamatik.api.ChatUsage
import com.llamatik.api.FunctionCall
import com.llamatik.api.ModelConfig
import com.llamatik.api.OllamaChatRequest
import com.llamatik.api.OllamaChatResponse
import com.llamatik.api.OllamaGenerateRequest
import com.llamatik.api.OllamaGenerateResponse
import com.llamatik.api.Tool
import com.llamatik.api.ToolCall
import com.llamatik.library.platform.LlamaService
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.random.Random

private const val DEFAULT_MAX_TOKENS = 512
private const val DEFAULT_TOP_P = 0.9f
private const val DEFAULT_TOP_K = 40
private const val DEFAULT_REPEAT_PENALTY = 1.1f
private const val UUID_SHORT_LENGTH = 8
private const val ZERO = 0
private const val MS_PER_SEC = 1000

@OptIn(kotlin.time.ExperimentalTime::class)
fun Route.openAiRoutes() {
    get("/v1/models") {
        call.respond(
            mapOf(
                "object" to "list",
                "data" to ModelConfig.recommendedModels.map {
                    mapOf(
                        "id" to it.id,
                        "object" to "model",
                        "created" to Clock.System.now().toEpochMilliseconds() / MS_PER_SEC,
                        "owned_by" to "llamatik"
                    )
                }
            )
        )
    }

    post("/v1/chat/completions") {
        val req = call.receive<ChatCompletionRequest>()
        val tools = req.tools
        val prompt = buildOpenAiPrompt(req.messages, tools)

        req.temperature?.let { t ->
            val maxTokens = req.maxTokens ?: DEFAULT_MAX_TOKENS
            val topP = req.topP ?: DEFAULT_TOP_P
            LlamaService.updateGenerateParams(t, maxTokens, topP, DEFAULT_TOP_K, DEFAULT_REPEAT_PENALTY)
        }

        val text = if (tools != null && tools.isNotEmpty()) {
            val schema = buildToolSchema(tools)
            LlamaService.generateJson(prompt, schema).getOrThrow()
        } else {
            LlamaService.generate(prompt).getOrThrow()
        }

        val message = if (tools != null && tools.isNotEmpty()) {
            parseToolResponse(text)
        } else {
            ChatMessage(role = "assistant", content = text)
        }

        val response = ChatCompletionResponse(
            id = "chatcmpl-" + Random.nextLong().toString(),
            created = Clock.System.now().toEpochMilliseconds() / MS_PER_SEC,
            model = req.model,
            choices = listOf(
                ChatChoice(
                    index = ZERO,
                    message = message,
                    finishReason = if (message.toolCalls != null) "tool_calls" else "stop"
                )
            ),
            usage = ChatUsage(ZERO, ZERO, ZERO)
        )
        call.respond(response)
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
fun Route.ollamaRoutes() {
    get("/api/tags") {
        call.respond(
            mapOf(
                "models" to ModelConfig.recommendedModels.map {
                    mapOf(
                        "name" to it.id,
                        "modified_at" to Clock.System.now().toString(),
                        "size" to 0,
                        "digest" to "sha256:0",
                        "details" to mapOf("family" to "llama")
                    )
                }
            )
        )
    }

    post("/api/generate") {
        val req = call.receive<OllamaGenerateRequest>()
        val text = LlamaService.generate(req.prompt).getOrThrow()
        val response = OllamaGenerateResponse(
            model = req.model,
            createdAt = Clock.System.now().toString(),
            response = text,
            done = true
        )
        call.respond(response)
    }

    post("/api/chat") {
        val req = call.receive<OllamaChatRequest>()
        val tools = req.tools
        val prompt = buildOpenAiPrompt(req.messages, tools)

        val text = if (tools != null && tools.isNotEmpty()) {
            val schema = buildToolSchema(tools)
            LlamaService.generateJson(prompt, schema).getOrThrow()
        } else {
            LlamaService.generate(prompt).getOrThrow()
        }

        val message = if (tools != null && tools.isNotEmpty()) {
            parseToolResponse(text)
        } else {
            ChatMessage(role = "assistant", content = text)
        }

        val response = OllamaChatResponse(
            model = req.model,
            createdAt = Clock.System.now().toString(),
            message = message,
            done = true
        )
        call.respond(response)
    }
}

@Suppress("UnusedParameter")
private fun buildOpenAiPrompt(messages: List<ChatMessage>, tools: List<Tool>? = null): String {
    val localTools = tools
    return buildString {
        if (localTools != null && localTools.isNotEmpty()) {
            append("<|im_start|>system\n")
            append("You are a helpful assistant with access to the following tools:\n")
            localTools.forEach { tool ->
                append("- ${tool.function.name}: ${tool.function.description}\n")
                append("  Parameters: ${tool.function.parameters}\n")
            }
            append("If you need to call a tool, respond with a JSON object containing 'tool_calls'.\n")
            append("<|im_end|>\n")
        }
        messages.forEach { msg ->
            append("<|im_start|>${msg.role}\n${msg.content ?: ""}")
            val tCalls = msg.toolCalls
            if (tCalls != null) {
                append("\nTool Calls: $tCalls")
            }
            append("<|im_end|>\n")
        }
        append("<|im_start|>assistant\n")
    }
}

@Suppress("UnusedParameter")
private fun buildToolSchema(tools: List<Tool>): String {
    if (tools.isEmpty()) return ""
    return """
    {
      "type": "object",
      "properties": {
        "content": { "type": "string" },
        "tool_calls": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "name": { "type": "string" },
              "arguments": { "type": "object" }
            },
            "required": ["name", "arguments"]
          }
        }
      }
    }
    """.trimIndent()
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
private fun parseToolResponse(jsonText: String): ChatMessage {
    return try {
        val json = Json.parseToJsonElement(jsonText).jsonObject
        val content = json["content"]?.jsonPrimitive?.content
        val toolCallsJson = json["tool_calls"]?.jsonArray

        val toolCalls = toolCallsJson?.map {
            val obj = it.jsonObject
            ToolCall(
                id = "call_" + Random.nextInt().toString().take(UUID_SHORT_LENGTH),
                type = "function",
                function = FunctionCall(
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    arguments = obj["arguments"]?.toString() ?: "{}"
                )
            )
        }

        ChatMessage(role = "assistant", content = content, toolCalls = toolCalls)
    } catch (e: Exception) {
        ChatMessage(role = "assistant", content = jsonText)
    }
}
