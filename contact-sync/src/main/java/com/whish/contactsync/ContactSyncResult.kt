package com.whish.contactsync

data class ContactSyncResult(
    val success: Boolean,
    val errorCode: ContactSyncErrorCode? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = ContactSyncResult(success = true)

        fun failure(
            errorCode: ContactSyncErrorCode,
            errorMessage: String
        ) = ContactSyncResult(
            success = false,
            errorCode = errorCode,
            errorMessage = errorMessage
        )
    }
}

enum class ContactSyncErrorCode {
    CONTACT_PERMISSION_NOT_GRANTED,
    NETWORK_ERROR,
    BACKEND_ERROR,
    DATABASE_ERROR,
    CONTACT_READ_ERROR,
    PENDING_SYNC_NOT_COMPLETED,
    UNKNOWN_ERROR
}