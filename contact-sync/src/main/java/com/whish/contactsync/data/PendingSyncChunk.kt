package com.whish.contactsync.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whish.contactsync.network.ContactPayloadDto

@Entity(tableName = "pending_sync_chunk")
data class PendingSyncChunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val syncType: String,
    val chunkIndex: Int,
    val added: List<ContactPayloadDto>,
    val updated: List<ContactPayloadDto>,
    val deleted: List<ContactPayloadDto>,
    val createdAtMillis: Long = System.currentTimeMillis()
)
