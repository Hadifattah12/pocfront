package com.whish.contactsync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.whish.contactsync.data.PendingSyncChunk
import com.whish.contactsync.network.OkHttpContactSyncApi
import com.whish.contactsync.network.SyncChunkRequestDto
import com.whish.contactsync.repository.ContactSyncRepository
import com.whish.contactsync.repository.toRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class ContactSyncManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val endpointUrl: String,
    private val headersProvider: () -> Map<String, String>
) {
    private val repository = ContactSyncRepository(context)
    private val apiService = OkHttpContactSyncApi(okHttpClient, endpointUrl)

    suspend fun syncContacts(): ContactSyncResult = withContext(Dispatchers.IO) {
        try {
            val hasContactsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasContactsPermission) {
                return@withContext ContactSyncResult.failure(
                    errorCode = ContactSyncErrorCode.CONTACT_PERMISSION_NOT_GRANTED,
                    errorMessage = "READ_CONTACTS permission is not granted"
                )
            }

            val currentHeaders = headersProvider()

            // Step 1: try sending any existing pending chunks first
            sendAllPendingUntilClear(currentHeaders)

            // If pending chunks still exist, do not compute new delta yet.
            // This prevents stale backend/local state.
            if (repository.getPendingChunks().isNotEmpty()) {
                return@withContext ContactSyncResult.failure(
                    errorCode = ContactSyncErrorCode.PENDING_SYNC_NOT_COMPLETED,
                    errorMessage = "Pending contact sync chunks could not be completed"
                )
            }

            // Step 2: compute fresh delta
            val delta = repository.processContactsAndComputeDelta()
            val totalChanges = delta.added.size + delta.updated.size + delta.deleted.size

            if (totalChanges == 0) {
                return@withContext ContactSyncResult.success()
            }

            // Step 3: create chunks, save each as pending, then send it
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

                tryOnce(currentHeaders, chunk)
            }

            // Retry anything still pending from Step 3 failures
            sendAllPendingUntilClear(currentHeaders)

            if (repository.getPendingChunks().isNotEmpty()) {
                return@withContext ContactSyncResult.failure(
                    errorCode = ContactSyncErrorCode.PENDING_SYNC_NOT_COMPLETED,
                    errorMessage = "Some contact sync chunks are still pending"
                )
            }

            ContactSyncResult.success()

        } catch (e: SecurityException) {
            ContactSyncResult.failure(
                errorCode = ContactSyncErrorCode.CONTACT_PERMISSION_NOT_GRANTED,
                errorMessage = e.message ?: "READ_CONTACTS permission is not granted"
            )
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Network or IO error during sync", e)
            ContactSyncResult.failure(
                errorCode = ContactSyncErrorCode.NETWORK_ERROR,
                errorMessage = e.message ?: "Network error"
            )
        } catch (e: android.database.SQLException) {
            Log.e(TAG, "Database error during sync", e)
            ContactSyncResult.failure(
                errorCode = ContactSyncErrorCode.DATABASE_ERROR,
                errorMessage = e.message ?: "Database error"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Contact sync failed", e)

            ContactSyncResult.failure(
                errorCode = ContactSyncErrorCode.UNKNOWN_ERROR,
                errorMessage = e.message ?: "Unknown contact sync error"
            )
        }
    }

    // Retries all pending chunks up to MAX_RETRIES passes per sync call.
    private suspend fun sendAllPendingUntilClear(headers: Map<String, String>) {
        var attempts = 0

        while (attempts < MAX_RETRIES) {
            val pending = repository.getPendingChunks()

            if (pending.isEmpty()) {
                return
            }

            for (chunk in pending) {
                tryOnce(headers, chunk)
            }

            attempts++

            if (repository.getPendingChunks().isNotEmpty()) {
                delay(RETRY_DELAY_MILLIS)
            }
        }
    }

    // Single attempt: success applies chunk to Room and removes pending chunk.
    // Failure leaves it pending for the next retry.
    private suspend fun tryOnce(
        headers: Map<String, String>,
        chunk: PendingSyncChunk
    ): Boolean {
        return try {
            val response = apiService.sendChunk(headers, chunk.toRequestDto())

            if (response.success) {
                repository.applySuccessfulPendingChunk(chunk)
                true
            } else {
                Log.w(
                    TAG,
                    "Chunk ${chunk.id} failed with ${response.errors.size} error(s); will retry."
                )
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