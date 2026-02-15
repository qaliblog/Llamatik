package com.llamatik.app.feature.server

import co.touchlab.kermit.Logger
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class ServerProvider {
    OPENAI, OLLAMA, BOTH
}

object ServerManager {
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startServer(port: Int, provider: ServerProvider) {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                embeddedServer(CIO, port = port, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                            encodeDefaults = true
                        })
                    }
                    routing {
                        if (provider == ServerProvider.OPENAI || provider == ServerProvider.BOTH) {
                            openAiRoutes()
                        }
                        if (provider == ServerProvider.OLLAMA || provider == ServerProvider.BOTH) {
                            ollamaRoutes()
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) {
                Logger.e("Server failed to start: ${e.message}")
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
    }

    fun isRunning(): Boolean = serverJob?.isActive == true
}
