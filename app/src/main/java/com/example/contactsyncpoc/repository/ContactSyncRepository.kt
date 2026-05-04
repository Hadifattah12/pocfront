package com.example.contactsyncpoc.repository

import android.content.Context
import com.example.contactsyncpoc.ContactReader
import com.example.contactsyncpoc.DeviceContact
import com.example.contactsyncpoc.data.AppDatabase
import com.example.contactsyncpoc.data.PendingSyncChunk
import com.example.contactsyncpoc.network.ContactPayloadDto
import com.example.contactsyncpoc.network.SyncChunkRequestDto
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactSyncRepository(private val context: Context) {

    private val contactDao = AppDatabase.getDatabase(context).contactDao()
    private val chunkDao = AppDatabase.getDatabase(context).pendingSyncChunkDao()

    suspend fun processContactsAndComputeDelta(): ContactDelta = withContext(Dispatchers.IO) {

        val addedContacts = mutableListOf<ContactPayloadDto>()
        val updatedContacts = mutableListOf<ContactPayloadDto>()
        val deletedContacts = mutableListOf<ContactPayloadDto>()

        Log.d("ContactSync", "Reading raw contacts from device...")
        val rawContacts = ContactReader.readPhoneNumbers(context)
        Log.d("ContactSync", "Found ${rawContacts.size} raw contacts.")

        Log.d("ContactSync", "Fetching previous snapshot from Room DB...")
        val previousSnapshots = contactDao.getAllContacts()
        val previousMapped = previousSnapshots.associateBy { it.phoneNumber }

        Log.d("ContactSync", "Room DB holds ${previousSnapshots.size} valid records.")

        val isInitialSync = previousSnapshots.isEmpty()

        // Map current device contacts by their raw phone number, handling duplicate numbers safely
        val currentMapped = mutableMapOf<String, DeviceContact>()
        for (contact in rawContacts) {
            val phone = contact.phoneNumber
            val previousSnapshot = previousMapped[phone]

            if (!currentMapped.containsKey(phone)) {
                currentMapped[phone] = contact
            } else {
                // If this duplicate's name matches what we ALREADY have in the DB, prefer this one
                // to avoid triggering a useless "Update".
                if (previousSnapshot != null && contact.displayName == previousSnapshot.displayName) {
                    currentMapped[phone] = contact
                }
            }
        }

        // 1. Find Added and Updated
        for ((phone, deviceContact) in currentMapped) {
            val previous = previousMapped[phone]
            val payload = ContactPayloadDto(deviceContact.phoneNumber, deviceContact.displayName)

            if (previous == null)
            {
                addedContacts.add(payload)
            }
            else if (previous.displayName != deviceContact.displayName)
            {
                updatedContacts.add(payload)
            }
        }

        // 2. Find Deleted
        val currentPhones = currentMapped.keys
        for ((phone, snapshot) in previousMapped)
        {
            if (!currentPhones.contains(phone))
            {
                deletedContacts.add(ContactPayloadDto(phone, snapshot.displayName))
            }
        }

        Log.d("ContactSync", "Sync Type: ${if (isInitialSync) "INITIAL" else "INCREMENTAL"} -> Added: ${addedContacts.size}, Updated: ${updatedContacts.size}, Deleted: ${deletedContacts.size}")

        ContactDelta(
            added = addedContacts,
            updated = if (isInitialSync) emptyList() else updatedContacts,
            deleted = if (isInitialSync) emptyList() else deletedContacts,
            isInitialSync = isInitialSync
        )
    }

    suspend fun applySuccessfulChunk(
        added: List<ContactPayloadDto>,
        updated: List<ContactPayloadDto>,
        deleted: List<ContactPayloadDto>
    ) = withContext<Unit>(Dispatchers.IO) {
        Log.d(
            "ContactSync",
            "Applying successful chunk locally -> Added: ${added.size}, Updated: ${updated.size}, Deleted: ${deleted.size}"
        )

        contactDao.applySuccessfulChunk(
            added = added,
            updated = updated,
            deleted = deleted
        )
        Log.d("ContactSync", "Successful chunk applied to Room.")
    }

    suspend fun getPendingChunks(): List<PendingSyncChunk> = withContext(Dispatchers.IO) {
        chunkDao.getPendingSyncChunks()
    }

    suspend fun savePendingChunk(request: SyncChunkRequestDto): PendingSyncChunk = withContext(Dispatchers.IO) {
        val chunk = PendingSyncChunk(
            syncType = request.syncType,
            chunkIndex = request.chunkIndex,
            added = request.added,
            updated = request.updated,
            deleted = request.deleted
        )
        val id = chunkDao.insertPendingSyncChunk(chunk)
        chunk.copy(id = id)
    }

    suspend fun applySuccessfulPendingChunk(chunk: PendingSyncChunk) = withContext<Unit>(Dispatchers.IO) {
        Log.d(
            "ContactSync",
            "Applying and removing pending chunk ${chunk.id} -> Added: ${chunk.added.size}, Updated: ${chunk.updated.size}, Deleted: ${chunk.deleted.size}"
        )
        contactDao.applySuccessfulChunk(
            added = chunk.added,
            updated = chunk.updated,
            deleted = chunk.deleted
        )
        chunkDao.deletePendingSyncChunk(chunk.id)
    }

}

fun PendingSyncChunk.toRequestDto(): SyncChunkRequestDto {
    return SyncChunkRequestDto(
        syncType = syncType,
        chunkIndex = chunkIndex,
        added = added,
        updated = updated,
        deleted = deleted
    )
}

data class ContactDelta(
    val added: List<ContactPayloadDto>,
    val updated: List<ContactPayloadDto>,
    val deleted: List<ContactPayloadDto>,
    val isInitialSync: Boolean
)
