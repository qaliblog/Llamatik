package com.llamatik.routes

import com.llamatik.API_VERSION
import com.llamatik.api.GenerateJsonRequest
import com.llamatik.api.GenerateJsonWithContextRequest
import com.llamatik.api.GenerateRequest
import com.llamatik.api.GenerateResponse
import com.llamatik.api.GenerateWithContextRequest
import com.llamatik.api.InitModelRequest
import com.llamatik.api.InitModelResponse
import com.llamatik.api.OkResponse
import com.llamatik.api.UpdateParamsRequest
import com.llamatik.library.platform.LlamaService
import com.llamatik.util.Sse
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val GENERATION = "$API_VERSION/generation"

private const val GENERATION_INIT = "$GENERATION/init"
private const val GENERATION_GENERATE = "$GENERATION/generate"
private const val GENERATION_GENERATE_WITH_CONTEXT = "$GENERATION/generateWithContext"
private const val GENERATION_GENERATE_JSON = "$GENERATION/generateJson"
private const val GENERATION_GENERATE_JSON_WITH_CONTEXT = "$GENERATION/generateJsonWithContext"
private const val GENERATION_STREAM = "$GENERATION/stream"
private const val GENERATION_STREAM_WITH_CONTEXT = "$GENERATION/streamWithContext"
private const val GENERATION_JSON_STREAM = "$GENERATION/jsonStream"
private const val GENERATION_JSON_STREAM_WITH_CONTEXT = "$GENERATION/jsonStreamWithContext"
private const val GENERATION_PARAMS = "$GENERATION/params"
private const val GENERATION_CANCEL = "$GENERATION/cancel"

/**
 * Mirrors the Llamatik library API for generation.
 */
@Suppress("TooGenericExceptionCaught", "LongMethod", "CyclomaticComplexMethod")
fun Route.generationRoutes() {
    // --- initialization ---
    post(GENERATION_INIT) {
        val req = call.receive<InitModelRequest>()
        val ok = LlamaService.initGenerateModel(req.modelPath).getOrThrow()
        call.respond(InitModelResponse(ok = ok))
    }

    // --- non-streaming ---
    post(GENERATION_GENERATE) {
        val req = call.receive<GenerateRequest>()
        val text = LlamaService.generate(req.prompt).getOrThrow()
        call.respond(GenerateResponse(text = text))
    }

    post(GENERATION_GENERATE_WITH_CONTEXT) {
        val req = call.receive<GenerateWithContextRequest>()
        val text = LlamaService.generateWithContext(req.systemPrompt, req.contextBlock, req.userPrompt).getOrThrow()
        call.respond(GenerateResponse(text = text))
    }

    post(GENERATION_GENERATE_JSON) {
        val req = call.receive<GenerateJsonRequest>()
        val text = LlamaService.generateJson(req.prompt, req.jsonSchema).getOrThrow()
        call.respond(GenerateResponse(text = text))
    }

    post(GENERATION_GENERATE_JSON_WITH_CONTEXT) {
        val req = call.receive<GenerateJsonWithContextRequest>()
        val text =
            LlamaService
                .generateJsonWithContext(
                    req.systemPrompt,
                    req.contextBlock,
                    req.userPrompt,
                    req.jsonSchema,
                ).getOrThrow()
        call.respond(GenerateResponse(text = text))
    }

    // --- params / cancel ---
    post(GENERATION_PARAMS) {
        val req = call.receive<UpdateParamsRequest>()
        LlamaService
            .updateGenerateParams(
                req.temperature,
                req.maxTokens,
                req.topP,
                req.topK,
                req.repeatPenalty,
            ).getOrThrow()
        call.respond(OkResponse())
    }

    post(GENERATION_CANCEL) {
        LlamaService.cancelGenerate()
        call.respond(OkResponse())
    }

    // --- streaming (SSE) ---
    post(GENERATION_STREAM) {
        val req = call.receive<GenerateRequest>()

        val deltas = Channel<String>(capacity = Channel.UNLIMITED)
        val done = Channel<Unit>(capacity = 1)
        val errors = Channel<String>(capacity = 1)

        val cb =
            Sse.genStreamCallback(
                onDelta = { deltas.trySend(it).isSuccess },
                onDone = { done.trySend(Unit).isSuccess },
                onError = { errors.trySend(it).isSuccess },
            )

        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    try {
                        LlamaService.generateStream(req.prompt, cb)
                    } catch (e: Throwable) {
                        errors.trySend(e.message ?: "Streaming failed")
                    }
                }

                Sse.pipe(
                    writer = this@respondTextWriter,
                    deltas = deltas,
                    done = done,
                    errors = errors,
                )
            }
        }
    }

    post(GENERATION_STREAM_WITH_CONTEXT) {
        val req = call.receive<GenerateWithContextRequest>()

        val deltas = Channel<String>(capacity = Channel.UNLIMITED)
        val done = Channel<Unit>(capacity = 1)
        val errors = Channel<String>(capacity = 1)

        val cb =
            Sse.genStreamCallback(
                onDelta = { deltas.trySend(it).isSuccess },
                onDone = { done.trySend(Unit).isSuccess },
                onError = { errors.trySend(it).isSuccess },
            )

        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    try {
                        LlamaService.generateStreamWithContext(req.systemPrompt, req.contextBlock, req.userPrompt, cb)
                    } catch (e: Throwable) {
                        errors.trySend(e.message ?: "Streaming failed")
                    }
                }

                Sse.pipe(writer = this@respondTextWriter, deltas = deltas, done = done, errors = errors)
            }
        }
    }

    post(GENERATION_JSON_STREAM) {
        val req = call.receive<GenerateJsonRequest>()

        val deltas = Channel<String>(capacity = Channel.UNLIMITED)
        val done = Channel<Unit>(capacity = 1)
        val errors = Channel<String>(capacity = 1)

        val cb =
            Sse.genStreamCallback(
                onDelta = { deltas.trySend(it).isSuccess },
                onDone = { done.trySend(Unit).isSuccess },
                onError = { errors.trySend(it).isSuccess },
            )

        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    try {
                        LlamaService.generateJsonStream(req.prompt, req.jsonSchema, cb)
                    } catch (e: Throwable) {
                        errors.trySend(e.message ?: "Streaming failed")
                    }
                }

                Sse.pipe(writer = this@respondTextWriter, deltas = deltas, done = done, errors = errors)
            }
        }
    }

    post(GENERATION_JSON_STREAM_WITH_CONTEXT) {
        val req = call.receive<GenerateJsonWithContextRequest>()

        val deltas = Channel<String>(capacity = Channel.UNLIMITED)
        val done = Channel<Unit>(capacity = 1)
        val errors = Channel<String>(capacity = 1)

        val cb =
            Sse.genStreamCallback(
                onDelta = { deltas.trySend(it).isSuccess },
                onDone = { done.trySend(Unit).isSuccess },
                onError = { errors.trySend(it).isSuccess },
            )

        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    try {
                        LlamaService.generateJsonStreamWithContext(
                            req.systemPrompt,
                            req.contextBlock,
                            req.userPrompt,
                            req.jsonSchema,
                            cb,
                        )
                    } catch (e: Throwable) {
                        errors.trySend(e.message ?: "Streaming failed")
                    }
                }

                Sse.pipe(writer = this@respondTextWriter, deltas = deltas, done = done, errors = errors)
            }
        }
    }
}
