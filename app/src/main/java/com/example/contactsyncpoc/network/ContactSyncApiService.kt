package com.example.contactsyncpoc.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ContactSyncApiService {

    @POST("/api/contacts/batch")
    suspend fun sendChunk(
        @Header("X-User-Id") userId: Long,
        @Body request: SyncChunkRequestDto
    ): SyncChunkResponse
}
