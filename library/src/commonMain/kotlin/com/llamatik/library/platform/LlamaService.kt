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
    private var isModelLoaded = false
    private var isGenerateModelLoaded = false

    suspend fun initModel(modelPath: String): Result<Boolean> = runCatching {
        mutex.withLock {
            val res = LlamaBridge.initModel(modelPath)
            isModelLoaded = res
            res
        }
    }

    suspend fun embed(input: String): Result<FloatArray> = runCatching {
        mutex.withLock {
            if (!isModelLoaded) throw IllegalStateException("Model not loaded")
            LlamaBridge.embed(input)
        }
    }

    suspend fun initGenerateModel(modelPath: String): Result<Boolean> = runCatching {
        mutex.withLock {
            val res = LlamaBridge.initGenerateModel(modelPath)
            isGenerateModelLoaded = res
            res
        }
    }

    suspend fun generate(prompt: String): Result<String> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
            LlamaBridge.generate(prompt)
        }
    }

    suspend fun generateWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
    ): Result<String> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
            LlamaBridge.generateWithContext(systemPrompt, contextBlock, userPrompt)
        }
    }

    suspend fun generateJson(
        prompt: String,
        jsonSchema: String?,
    ): Result<String> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
            LlamaBridge.generateJson(prompt, jsonSchema)
        }
    }

    suspend fun generateJsonWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        jsonSchema: String?,
    ): Result<String> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
            LlamaBridge.generateJsonWithContext(systemPrompt, contextBlock, userPrompt, jsonSchema)
        }
    }

    suspend fun updateGenerateParams(
        temperature: Float,
        maxTokens: Int,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
    ): Result<Unit> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) return@runCatching // Silently ignore if not loaded yet
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
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
            LlamaBridge.generateStream(prompt, callback)
        }
    }

    suspend fun generateStreamWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        callback: GenStream
    ): Result<Unit> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
            LlamaBridge.generateStreamWithContext(systemPrompt, contextBlock, userPrompt, callback)
        }
    }

    suspend fun generateJsonStream(prompt: String, jsonSchema: String?, callback: GenStream): Result<Unit> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
            LlamaBridge.generateJsonStream(prompt, jsonSchema, callback)
        }
    }

    suspend fun generateJsonStreamWithContext(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        jsonSchema: String?,
        callback: GenStream
    ): Result<Unit> = runCatching {
        mutex.withLock {
            if (!isGenerateModelLoaded) throw IllegalStateException("Generate model not loaded")
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
        mutex.withLock {
            LlamaBridge.shutdown()
            isModelLoaded = false
            isGenerateModelLoaded = false
        }
    }
}
