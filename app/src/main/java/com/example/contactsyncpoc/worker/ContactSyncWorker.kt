package com.example.contactsyncpoc.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.contactsyncpoc.network.ApiClient
import com.example.contactsyncpoc.network.SyncChunkRequestDto
import com.example.contactsyncpoc.network.SyncSessionRequest
import com.example.contactsyncpoc.repository.ContactSyncRepository
import java.text.SimpleDateFormat
import java.util.*

class ContactSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val repository = ContactSyncRepository(appContext)
    private val apiService = ApiClient.contactSyncApiService

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try
        {
            val clientSyncId = inputData.getString("clientSyncId") ?: throw IllegalStateException("Missing clientSyncId")
            val delta = repository.processContactsAndComputeDelta()

            val totalChanges = delta.added.size + delta.deleted.size + delta.updated.size
            if (totalChanges == 0)
            {
                return androidx.work.ListenableWorker.Result.success()
            }

            // A placeholder user ID until you implement real auth.
            val currentUserId = 1L

            // 1. Create Sync Session
            val sessionRequest = SyncSessionRequest(
                clientSyncId = clientSyncId,
                syncType = if (delta.isInitialSync) "INITIAL" else "INCREMENTAL",
                totalChanges = totalChanges
            )

            val sessionResponse = apiService.createSyncSession(currentUserId, sessionRequest)
            val backendSessionId = sessionResponse.sessionId
            val chunkSize = sessionResponse.chunkSize

            val allAdded = delta.added
            val allDeleted = delta.deleted
            val allUpdated = delta.updated

            val numChunks = maxOf(
                (allAdded.size + chunkSize - 1) / chunkSize,
                (allDeleted.size + chunkSize - 1) / chunkSize,
                (allUpdated.size + chunkSize - 1) / chunkSize
            ).coerceAtLeast(1)

            for (i in 0 until numChunks)
            {
                val addedChunk = allAdded.drop(i * chunkSize).take(chunkSize)
                val deletedChunk = allDeleted.drop(i * chunkSize).take(chunkSize)
                val updatedChunk = allUpdated.drop(i * chunkSize).take(chunkSize)

                val chunkRequest = SyncChunkRequestDto(
                    chunkIndex = i,
                    added = addedChunk,
                    deleted = deletedChunk,
                    updated = updatedChunk
                )

                var chunkSuccess = false
                var retryCount = 0
                val maxRetries = 3

                while (!chunkSuccess && retryCount <= maxRetries) {
                    try {
                        val chunkResponse = apiService.sendChunk(currentUserId, backendSessionId, chunkRequest)
                        if (chunkResponse.status == "COMPLETED" || chunkResponse.status == "received") {
                            chunkSuccess = true
                        } else {
                            throw Exception("Unexpected chunk status: ${chunkResponse.status}")
                        }
                    } catch (e: Exception) {
                        retryCount++
                        if (retryCount > maxRetries) {
                            throw Exception("API failed to process chunk ${chunkRequest.chunkIndex} after $maxRetries retries. Last error: ${e.message}")
                        }
                        // Optional: delay before retry
                        kotlinx.coroutines.delay(2000L)
                    }
                }
            }

            // 3. Update Snapshot only after all chunks are sent successfully
            repository.updateLocalSnapshot(delta.currentSnapshotEntities)

            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            androidx.work.ListenableWorker.Result.retry()
        }
    }
}
