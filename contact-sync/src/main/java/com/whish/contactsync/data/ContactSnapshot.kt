package com.whish.contactsync.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_snapshot")
data class ContactSnapshot(
    @PrimaryKey val phoneNumber: String,
    val displayName: String?
)
