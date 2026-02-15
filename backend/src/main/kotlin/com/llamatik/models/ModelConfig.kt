package com.llamatik.models

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val url: String,
    val supportsToolCalling: Boolean = true,
)

object ModelConfig {
    val recommendedModels =
        listOf(
            ModelInfo(
                id = "llama-3.1-8b-instruct",
                name = "Llama 3.1 8B Instruct",
                description = "Meta's latest 8B model with improved tool calling and context.",
                author = "Meta",
                url = "https://huggingface.co/bartowski/Llama-3.1-8B-Instruct-GGUF",
            ),
            ModelInfo(
                id = "hermes-3-llama-3.1-8b",
                name = "Hermes 3 - Llama 3.1 8B",
                description = "Fine-tuned version of Llama 3.1 by Nous Research, excellent at tool calling.",
                author = "Nous Research",
                url = "https://huggingface.co/NousResearch/Hermes-3-Llama-3.1-8B-GGUF",
            ),
            ModelInfo(
                id = "qwen-2.5-7b-instruct",
                name = "Qwen 2.5 7B Instruct",
                description = "Alibaba's latest model, very strong performance for its size.",
                author = "Alibaba Cloud",
                url = "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF",
            ),
        )
}
