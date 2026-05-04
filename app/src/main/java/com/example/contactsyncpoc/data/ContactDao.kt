package com.example.contactsyncpoc.data

import androidx.room.*
import com.example.contactsyncpoc.network.ContactPayloadDto

@Dao
abstract class ContactDao {
    @Query("SELECT * FROM contact_snapshot")
    abstract suspend fun getAllContacts(): List<ContactSnapshot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(contacts: List<ContactSnapshot>)

    @Query("DELETE FROM contact_snapshot WHERE phoneNumber IN (:phoneNumbers)")
    abstract suspend fun deleteByPhoneNumbers(phoneNumbers: List<String>): Int

    @Query("DELETE FROM contact_snapshot")
    abstract suspend fun deleteAll(): Int

    @Query("SELECT * FROM pending_sync_chunk ORDER BY id ASC")
    abstract suspend fun getPendingSyncChunks(): List<PendingSyncChunk>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertPendingSyncChunk(chunk: PendingSyncChunk): Long

    @Query("DELETE FROM pending_sync_chunk WHERE id = :id")
    abstract suspend fun deletePendingSyncChunk(id: Long): Int

    @Transaction
    open suspend fun updateSnapshot(contacts: List<ContactSnapshot>) {
        deleteAll()
        insertAll(contacts)
    }

    @Transaction
    open suspend fun applySuccessfulChunk(
        added: List<ContactPayloadDto>,
        updated: List<ContactPayloadDto>,
        deleted: List<ContactPayloadDto>
    ) {
        val contactsToUpsert = (added + updated).map { contact ->
            ContactSnapshot(
                phoneNumber = contact.phoneNumber,
                displayName = contact.displayName
            )
        }
        val phonesToDelete = deleted.map { it.phoneNumber }

        if (contactsToUpsert.isNotEmpty()) {
            insertAll(contactsToUpsert)
        }

        if (phonesToDelete.isNotEmpty()) {
            deleteByPhoneNumbers(phonesToDelete)
        }
    }

    @Transaction
    open suspend fun applySuccessfulPendingChunk(chunk: PendingSyncChunk) {
        applySuccessfulChunk(
            added = chunk.added,
            updated = chunk.updated,
            deleted = chunk.deleted
        )
        deletePendingSyncChunk(chunk.id)
    }
}
