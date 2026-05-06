package com.whish.contactsync.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

internal class OkHttpContactSyncApi(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val gson: Gson = Gson()
) {
    suspend fun sendChunk(
        userId: Long,
        requestDto: SyncChunkRequestDto
    ): SyncChunkResponse = withContext(Dispatchers.IO) {
        val json = gson.toJson(requestDto)

        val body = json.toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/contacts/batch")
            .addHeader("X-User-Id", userId.toString())
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body.string()

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $responseBody")
            }

            gson.fromJson(responseBody, SyncChunkResponse::class.java)
        }
    }
}