package com.example.contactsyncpoc.network

import com.google.gson.annotations.SerializedName

data class ContactPayloadDto(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("contactName")
    val displayName: String?
)

data class SyncChunkRequestDto(
    @SerializedName("syncType")
    val syncType: String,
    @SerializedName("chunkIndex")
    val chunkIndex: Int,
    @SerializedName("added")
    val added: List<ContactPayloadDto>,
    @SerializedName("updated")
    val updated: List<ContactPayloadDto>,
    @SerializedName("deleted")
    val deleted: List<ContactPayloadDto>
)

data class SyncChunkResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("received")
    val received: Int,
    @SerializedName("errors")
    val errors: List<ContactUploadError>
)

data class ContactUploadError(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("reason")
    val reason: String
)
