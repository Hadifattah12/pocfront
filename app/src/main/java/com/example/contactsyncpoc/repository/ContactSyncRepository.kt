package com.example.contactsyncpoc.repository

import android.content.Context
import com.example.contactsyncpoc.ContactReader
import com.example.contactsyncpoc.data.AppDatabase
import com.example.contactsyncpoc.data.ContactSnapshot
import com.example.contactsyncpoc.network.ContactPayloadDto
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactSyncRepository(private val context: Context) {

    private val contactDao = AppDatabase.getDatabase(context).contactDao()

    suspend fun processContactsAndComputeDelta(): ContactDelta = withContext(Dispatchers.IO) {

        val addedContacts = mutableListOf<ContactPayloadDto>()
        val deletedContacts = mutableListOf<ContactPayloadDto>()
        val updatedContacts = mutableListOf<ContactPayloadDto>()

        Log.d("ContactSync", "Reading raw contacts from device...")
        val rawContacts = ContactReader.readPhoneNumbers(context)
        Log.d("ContactSync", "Found ${rawContacts.size} raw contacts.")

        Log.d("ContactSync", "Fetching previous snapshot from Room DB...")
        val previousSnapshots = contactDao.getAllContacts()
        val previousMapped = previousSnapshots.associateBy { it.phoneNumber }

        Log.d("ContactSync", "Room DB holds ${previousSnapshots.size} valid records.")

        val isInitialSync = previousSnapshots.isEmpty()

        // Map current device contacts by their raw phone number, handling duplicate numbers safely
        val currentMapped = mutableMapOf<String, com.example.contactsyncpoc.DeviceContact>()
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

        Log.d("ContactSync", "Sync Type: ${if (isInitialSync) "INITIAL" else "DELTA"} -> Added: ${addedContacts.size}, Deleted: ${deletedContacts.size}, Updated: ${updatedContacts.size}")

        // Build the new snapshot that will replace the local DB only after sync finishes
        val newSnapshot = currentMapped.map { (phone, contact) ->
            ContactSnapshot(phone, contact.displayName)
        }

        ContactDelta(
            added = addedContacts,
            deleted = if (isInitialSync) emptyList() else deletedContacts,
            updated = if (isInitialSync) emptyList() else updatedContacts,
            currentSnapshotEntities = newSnapshot,
            isInitialSync = isInitialSync
        )
    }

    suspend fun updateLocalSnapshot(snapshotEntities: List<ContactSnapshot>) = withContext<Unit>(Dispatchers.IO) {
        Log.d("ContactSync", "Updating Room DB with new snapshot of size ${snapshotEntities.size}...")
        contactDao.updateSnapshot(snapshotEntities)
        Log.d("ContactSync", "Room DB update complete.")
    }
}

data class ContactDelta(
    val added: List<ContactPayloadDto>,
    val deleted: List<ContactPayloadDto>,
    val updated: List<ContactPayloadDto>,
    val currentSnapshotEntities: List<ContactSnapshot>,
    val isInitialSync: Boolean
)
