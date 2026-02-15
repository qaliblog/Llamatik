package com.llamatik.repository.generation

import com.llamatik.library.platform.LlamaService

class GenerationRepositoryImpl : GenerationRepository {
    override suspend fun generate(prompt: String): Result<String> = LlamaService.generate(prompt)
}
