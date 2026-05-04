package com.example.contactsyncpoc.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import com.example.contactsyncpoc.network.ApiClient
import com.example.contactsyncpoc.network.SyncChunkRequestDto
import com.example.contactsyncpoc.data.PendingSyncChunk
import com.example.contactsyncpoc.repository.ContactSyncRepository
import com.example.contactsyncpoc.repository.toRequestDto

class ContactSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val repository = ContactSyncRepository(appContext)
    private val apiService = ApiClient.contactSyncApiService

    override suspend fun doWork(): Result {
        return try {
            val currentUserId = 1L

            // Step 1: attempt all leftover pending chunks from previous runs (one try each)
            attemptAllPending(currentUserId)

            // Step 2: compute delta — snapshot is now accurate after step 1 success.
            val delta = repository.processContactsAndComputeDelta()
            val totalChanges = delta.added.size + delta.updated.size + delta.deleted.size

            // Step 3: for each new chunk, save it then immediately attempt to send it
            if (totalChanges > 0) {
                val syncType = if (delta.isInitialSync) "INITIAL" else "INCREMENTAL"
                val numChunks = maxOf(
                    (delta.added.size + LOCAL_CHUNK_SIZE - 1) / LOCAL_CHUNK_SIZE,
                    (delta.updated.size + LOCAL_CHUNK_SIZE - 1) / LOCAL_CHUNK_SIZE,
                    (delta.deleted.size + LOCAL_CHUNK_SIZE - 1) / LOCAL_CHUNK_SIZE
                ).coerceAtLeast(1)

                for (i in 0 until numChunks) {
                    val chunk = repository.savePendingChunk(
                        SyncChunkRequestDto(
                            syncType = syncType,
                            chunkIndex = i,
                            added = delta.added.drop(i * LOCAL_CHUNK_SIZE).take(LOCAL_CHUNK_SIZE),
                            updated = delta.updated.drop(i * LOCAL_CHUNK_SIZE).take(LOCAL_CHUNK_SIZE),
                            deleted = delta.deleted.drop(i * LOCAL_CHUNK_SIZE).take(LOCAL_CHUNK_SIZE)
                        )
                    )
                    tryOnce(currentUserId, chunk)
                }
            }

            // Step 4: retry anything still pending (step 1 + step 3 failures) until clear.
            // Chunks that fail MAX_RETRIES times this run are skipped and left in Room
            // for the next worker run.
            sendAllPendingUntilClear(currentUserId)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // One pass over existing pending chunks — single attempt each, no blocking
    private suspend fun attemptAllPending(userId: Long) {
        val pending = repository.getPendingChunks()
        for (chunk in pending) {
            tryOnce(userId, chunk)
        }
    }

    // Retries all pending chunks up to MAX_RETRIES passes per worker run.
    // Any chunks still pending after that are left in Room for the next run.
    private suspend fun sendAllPendingUntilClear(userId: Long) {
        var attempts = 0
        while (attempts < MAX_RETRIES) {
            val pending = repository.getPendingChunks()
            if (pending.isEmpty()) return
            for (chunk in pending) {
                tryOnce(userId, chunk)
            }
            attempts++
            if (repository.getPendingChunks().isNotEmpty()) {
                delay(RETRY_DELAY_MILLIS)
            }
        }
    }

    // Single attempt — success removes chunk from Room, failure leaves it for the next retry pass
    private suspend fun tryOnce(userId: Long, chunk: PendingSyncChunk): Boolean {
        return try {
            val response = apiService.sendChunk(userId, chunk.toRequestDto())
            if (response.success) {
                repository.applySuccessfulPendingChunk(chunk)
                true
            } else {
                Log.w(TAG, "Chunk ${chunk.id} failed with ${response.errors.size} error(s); will retry.")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Chunk ${chunk.id} failed; will retry.", e)
            false
        }
    }

    private companion object {
        const val TAG = "ContactSync"
        const val LOCAL_CHUNK_SIZE = 100
        const val RETRY_DELAY_MILLIS = 2_000L
        const val MAX_RETRIES = 5
    }
}
