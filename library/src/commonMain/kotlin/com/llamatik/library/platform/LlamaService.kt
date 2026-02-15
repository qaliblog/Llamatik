package com.llamatik.library.platform

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Façade over the KMP [LlamaBridge] that ensures thread safety.
 *
 * The llama.cpp JNI integration is typically not re-entrant. This service
 * serializes calls that mutate or use the current generation session.
 */
object LlamaService {
    private val mutex = Mutex()

    suspend fun initModel(modelPath: String): Result<Boolean> = runCatching {
        mutex.withLock { LlamaBridge.initModel(modelPath) }
    }

    suspend fun embed(input: String): Result<FloatArray> = runCatching {
        mutex.withLock { LlamaBridge.embed(input) }
    }

    suspend fun initGenerateModel(modelPath: String): Result<Boolean> = runCatching {
        mutex.withLock { LlamaBridge.initGenerateModel(modelPath) }
    }

    suspend fun generate(prompt: String): Result<String> = runCatching {
        mutex.withLock { LlamaBridge.generate(prompt) }
    }

    suspend fun generateWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
    ): Result<String> = runCatching {
        mutex.withLock { LlamaBridge.generateWithContext(systemPrompt, contextBlock, userPrompt) }
    }

    suspend fun generateJson(
        prompt: String,
        jsonSchema: String?,
    ): Result<String> = runCatching {
        mutex.withLock { LlamaBridge.generateJson(prompt, jsonSchema) }
    }

    suspend fun generateJsonWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        jsonSchema: String?,
    ): Result<String> = runCatching {
        mutex.withLock { LlamaBridge.generateJsonWithContext(systemPrompt, contextBlock, userPrompt, jsonSchema) }
    }

    suspend fun updateGenerateParams(
        temperature: Float,
        maxTokens: Int,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
    ): Result<Unit> = runCatching {
        mutex.withLock {
            LlamaBridge.updateGenerateParams(
                temperature = temperature,
                maxTokens = maxTokens,
                topP = topP,
                topK = topK,
                repeatPenalty = repeatPenalty,
            )
        }
    }

    suspend fun generateStream(prompt: String, callback: GenStream): Result<Unit> = runCatching {
        mutex.withLock { LlamaBridge.generateStream(prompt, callback) }
    }

    suspend fun generateStreamWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        callback: GenStream
    ): Result<Unit> = runCatching {
        mutex.withLock { LlamaBridge.generateStreamWithContext(systemPrompt, contextBlock, userPrompt, callback) }
    }

    suspend fun generateJsonStream(prompt: String, jsonSchema: String?, callback: GenStream): Result<Unit> = runCatching {
        mutex.withLock { LlamaBridge.generateJsonStream(prompt, jsonSchema, callback) }
    }

    suspend fun generateJsonStreamWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        jsonSchema: String?,
        callback: GenStream
    ): Result<Unit> = runCatching {
        mutex.withLock {
            LlamaBridge.generateJsonStreamWithContext(systemPrompt, contextBlock, userPrompt, jsonSchema, callback)
        }
    }

    /**
     * Cancel does NOT use the lock so it can interrupt a locked generation.
     */
    fun cancelGenerate() {
        LlamaBridge.nativeCancelGenerate()
    }

    suspend fun shutdown(): Result<Unit> = runCatching {
        mutex.withLock { LlamaBridge.shutdown() }
    }
}
