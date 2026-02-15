package com.llamatik

import com.llamatik.plugins.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() =
        testApplication {
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
    fun testModels() =
        testApplication {
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
