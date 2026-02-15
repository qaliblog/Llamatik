package com.llamatik.app.feature.chatbot.utils

import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaService
import kotlin.math.min

/**
 * High-level chat orchestration:
 * - Renders the prompt with PromptRenderer (+ template + RAG).
 * - Streams tokens from LlamaService.generateStream.
 * - Enforces client-side stop sequences consistently across apps.
 */
object ChatRunner {

    /**
     * Stream a chat turn.
     *
     * @param system Optional system prompt (defaults to a safe helper system).
     * @param contexts RAG passages (already ranked/shortened).
     * @param messages Chat history (last one should be the user turn we’re answering).
     * @param template Which prompt surface to use (Gemma3 by default).
     * @param maxTokens Hard guard if your engine doesn't supply one.
     */
    suspend fun stream(
        system: String? = null,
        contexts: List<String> = emptyList(),
        messages: List<ChatMessage>,
        template: PromptTemplate = Gemma3,
        maxTokens: Int = 1024,
        onDelta: (String) -> Unit,
        onComplete: (final: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val prompt = PromptRenderer.render(system, contexts, messages, template)
        val stop = template.stopSequences

        val acc = StringBuilder()
        var done = false
        var tokenCount = 0

        // Thin wrapper that enforces consistent stop logic above the JNI layer.
        val guard = object : GenStream {
            override fun onDelta(text: String) {
                if (done) return

                val t = text.ifEmpty { return }
                acc.append(t)
                tokenCount++

                // Client-side stop: cheap suffix check
                if (shouldStop(acc, stop)) {
                    done = true
                    onComplete(trimAtStop(acc.toString(), stop))
                    return
                }

                if (tokenCount >= maxTokens) {
                    done = true
                    onComplete(acc.toString())
                    return
                }

                onDelta(t)
            }

            override fun onComplete() {
                if (!done) {
                    done = true
                    onComplete(acc.toString())
                }
            }

            override fun onError(message: String) {
                if (!done) {
                    done = true
                    onError(message)
                }
            }
        }

        // Let the engine do its thing; we keep the semantics above it.
        LlamaService.generateStream(prompt, guard)
    }

    private fun shouldStop(sb: StringBuilder, stops: List<String>): Boolean {
        if (stops.isEmpty()) return false
        val s = sb.toString()
        // Only check the last ~64 chars for performance
        val tail = s.takeLast(min(64, s.length))
        return stops.any { tail.endsWith(it) }
    }

    private fun trimAtStop(text: String, stops: List<String>): String {
        var out = text
        for (s in stops) {
            val idx = out.lastIndexOf(s)
            if (idx >= 0) {
                out = out.substring(0, idx)
                break
            }
        }
        return out
    }
}
