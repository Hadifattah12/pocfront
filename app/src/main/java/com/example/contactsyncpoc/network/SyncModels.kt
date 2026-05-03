package com.example.contactsyncpoc.network

import com.google.gson.annotations.SerializedName

data class ContactPayloadDto(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("contactName")
    val displayName: String?
)

data class SyncSessionRequest(
    @SerializedName("clientSyncId")
    val clientSyncId: String,
    @SerializedName("syncType")
    val syncType: String,
    @SerializedName("totalChanges")
    val totalChanges: Int
)

data class SyncSessionResponse(
    @SerializedName("sessionId")
    val sessionId: Long,
    @SerializedName("clientSyncId")
    val clientSyncId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("chunkSize")
    val chunkSize: Int,
    @SerializedName("totalExpectedChunks")
    val totalExpectedChunks: Int,
    @SerializedName("expiresAt")
    val expiresAt: String
)

data class SyncChunkRequestDto(
    @SerializedName("chunkIndex")
    val chunkIndex: Int,
    @SerializedName("region")
    val region: String? = null,
    @SerializedName("added")
    val added: List<ContactPayloadDto>,
    @SerializedName("deleted")
    val deleted: List<ContactPayloadDto>,
    @SerializedName("updated")
    val updated: List<ContactPayloadDto>
)

data class SyncChunkResponse(
    @SerializedName("accepted")
    val accepted: Boolean,
    @SerializedName("chunkIndex")
    val chunkIndex: Int,
    @SerializedName("status")
    val status: String
)
