package com.llamatik

import com.llamatik.plugins.configureSerialization
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureSerialization()
            configureGeneralRouting()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Welcome to Llamatik Server!", bodyAsText())
        }
    }

    @Test
    fun testModels() = testApplication {
        application {
            configureSerialization()
            configureGeneralRouting()
        }
        client.get("/v1/models").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(bodyAsText().contains("llama-3.1-8b-instruct"))
        }
    }
}
