package com.llamatik.routes

import com.llamatik.API_VERSION
import com.llamatik.api.EmbedRequest
import com.llamatik.api.EmbedResponse
import com.llamatik.library.platform.LlamaService
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

const val EMBEDDINGS = "$API_VERSION/embeddings"
const val EMBEDDINGS_INIT = "$EMBEDDINGS/init"
const val EMBEDDINGS_EMBED = "$EMBEDDINGS/embed"

/**
 * Mirrors the Llamatik library API for embeddings.
 *
 * POST /v1/embeddings/embed
 */
fun Route.embeddingRoutes() {
    post(EMBEDDINGS_INIT) {
        val req = call.receive<com.llamatik.api.InitModelRequest>()
        val ok = LlamaService.initModel(req.modelPath).getOrThrow()
        call.respond(com.llamatik.api.InitModelResponse(ok = ok))
    }

    post(EMBEDDINGS_EMBED) {
        val req = call.receive<EmbedRequest>()
        val embedding = LlamaService.embed(req.input).getOrThrow()
        call.respond(EmbedResponse(embedding = embedding.toList()))
    }
}
