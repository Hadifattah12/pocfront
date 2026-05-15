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
    private val endpointUrl: String,
    private val gson: Gson = Gson()
) {
    suspend fun sendChunk(
        headers: Map<String, String>,
        requestDto: SyncChunkRequestDto
    ): SyncChunkResponse = withContext(Dispatchers.IO) {
        val json = gson.toJson(requestDto)

        val body = json.toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val requestBuilder = Request.Builder()
            .url(endpointUrl)
            .post(body)

        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body.string()

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $responseBody")
            }

            gson.fromJson(responseBody, SyncChunkResponse::class.java)
        }
    }
}