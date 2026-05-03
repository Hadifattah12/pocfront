package com.example.contactsyncpoc.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactSyncApiService {

    @POST("contact-sync/sessions")
    suspend fun createSyncSession(
        @Header("X-User-Id") userId: Long,
        @Body request: SyncSessionRequest
    ): SyncSessionResponse

    @POST("contact-sync/sessions/{sessionId}/chunks")
    suspend fun sendChunk(
        @Header("X-User-Id") userId: Long,
        @Path("sessionId") sessionId: Long,
        @Body request: SyncChunkRequestDto
    ): SyncChunkResponse
}
