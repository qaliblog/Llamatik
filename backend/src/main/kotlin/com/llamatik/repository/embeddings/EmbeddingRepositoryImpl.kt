package com.llamatik.repository.embeddings

import com.llamatik.library.platform.LlamaService

class EmbeddingRepositoryImpl : EmbeddingRepository {
    override suspend fun getEmbedding(input: String): Result<FloatArray> = LlamaService.embed(input)
}
