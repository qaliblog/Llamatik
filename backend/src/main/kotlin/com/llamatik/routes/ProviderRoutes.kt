package com.llamatik.routes

import com.llamatik.api.ChatChoice
import com.llamatik.api.ChatCompletionRequest
import com.llamatik.api.ChatCompletionResponse
import com.llamatik.api.ChatMessage
import com.llamatik.api.ChatUsage
import com.llamatik.api.FunctionCall
import com.llamatik.api.OllamaChatRequest
import com.llamatik.api.OllamaChatResponse
import com.llamatik.api.OllamaGenerateRequest
import com.llamatik.api.OllamaGenerateResponse
import com.llamatik.api.Tool
import com.llamatik.api.ToolCall
import com.llamatik.llama.LlamaService
import com.llamatik.models.ModelConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val DEFAULT_MAX_TOKENS = 512
private const val DEFAULT_TEMPERATURE = 1.0f
private const val DEFAULT_TOP_P = 0.9f
private const val DEFAULT_TOP_K = 40
private const val DEFAULT_REPEAT_PENALTY = 1.1f

fun Route.providerRoutes() {
    openAiRoutes()
    ollamaRoutes()
}

fun Route.openAiRoutes() {
    get("/v1/models") {
        call.respond(mapOf("data" to ModelConfig.recommendedModels))
    }

    post("/v1/chat/completions") {
        val req = call.receive<ChatCompletionRequest>()
        val prompt = buildOpenAiPrompt(req.messages, req.tools)

        req.temperature?.let { t ->
            val maxTokens = req.maxTokens ?: DEFAULT_MAX_TOKENS
            val topP = req.topP ?: DEFAULT_TOP_P
            LlamaService.updateGenerateParams(t, maxTokens, topP, DEFAULT_TOP_K, DEFAULT_REPEAT_PENALTY)
        }

        val res = if (req.tools != null && req.tools.isNotEmpty()) {
            val schema = buildToolSchema(req.tools)
            LlamaService.generateJson(prompt, schema)
        } else {
            LlamaService.generate(prompt)
        }

        res.fold(
            onSuccess = { text ->
                val message = if (req.tools != null && req.tools.isNotEmpty()) {
                    parseToolResponse(text)
                } else {
                    ChatMessage(role = "assistant", content = text)
                }

                val response = ChatCompletionResponse(
                    id = "chatcmpl-" + UUID.randomUUID().toString(),
                    created = Clock.System.now().epochSeconds,
                    model = req.model,
                    choices = listOf(
                        ChatChoice(
                            index = 0,
                            message = message,
                            finishReason = if (message.toolCalls != null) "tool_calls" else "stop"
                        )
                    ),
                    usage = ChatUsage(0, 0, 0)
                )
                call.respond(response)
            },
            onFailure = {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Generation failed")
            }
        )
    }
}

fun Route.ollamaRoutes() {
    post("/api/generate") {
        val req = call.receive<OllamaGenerateRequest>()
        val res = LlamaService.generate(req.prompt)
        res.fold(
            onSuccess = { text ->
                val response = OllamaGenerateResponse(
                    model = req.model,
                    createdAt = Clock.System.now().toString(),
                    response = text,
                    done = true
                )
                call.respond(response)
            },
            onFailure = {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Generation failed")
            }
        )
    }

    get("/api/tags") {
        val models = ModelConfig.recommendedModels.map {
            mapOf(
                "name" to it.id,
                "model" to it.id,
                "details" to mapOf("family" to "llama")
            )
        }
        call.respond(mapOf("models" to models))
    }

    post("/api/chat") {
        val req = call.receive<OllamaChatRequest>()
        val prompt = buildOpenAiPrompt(req.messages, req.tools)

        val res = if (req.tools != null && req.tools.isNotEmpty()) {
            val schema = buildToolSchema(req.tools)
            LlamaService.generateJson(prompt, schema)
        } else {
            LlamaService.generate(prompt)
        }

        res.fold(
            onSuccess = { text ->
                val message = if (req.tools != null && req.tools.isNotEmpty()) {
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
            },
            onFailure = {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Generation failed")
            }
        )
    }
}

private fun buildOpenAiPrompt(messages: List<ChatMessage>, tools: List<Tool>? = null): String {
    return buildString {
        if (tools != null && tools.isNotEmpty()) {
            append("<|im_start|>system\n")
            append("You are a helpful assistant with access to the following tools:\n")
            tools.forEach { tool ->
                append("- ${tool.function.name}: ${tool.function.description}\n")
                append("  Parameters: ${tool.function.parameters}\n")
            }
            append("If you need to call a tool, respond with a JSON object containing 'tool_calls'.\n")
            append("<|im_end|>\n")
        }
        messages.forEach { msg ->
            append("<|im_start|>${msg.role}\n${msg.content ?: ""}")
            if (msg.toolCalls != null) {
                append("\nTool Calls: ${msg.toolCalls}")
            }
            append("<|im_end|>\n")
        }
        append("<|im_start|>assistant\n")
    }
}

private fun buildToolSchema(tools: List<Tool>): String {
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
                id = "call_" + UUID.randomUUID().toString().take(8),
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
