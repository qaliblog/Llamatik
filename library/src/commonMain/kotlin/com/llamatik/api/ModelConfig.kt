package com.llamatik.api

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val toolCalling: Boolean
)

object ModelConfig {
    val recommendedModels = listOf(
        ModelInfo("llama-3.1-8b-instruct", "Llama 3.1 8B", "Meta Llama 3.1 8B Instruct", true),
        ModelInfo("hermes-3-llama-3.1-8b", "Hermes 3 8B", "Nous Hermes 3 Llama 3.1 8B", true),
        ModelInfo("qwen-2.5-7b-instruct", "Qwen 2.5 7B", "Qwen 2.5 7B Instruct", true)
    )
}
