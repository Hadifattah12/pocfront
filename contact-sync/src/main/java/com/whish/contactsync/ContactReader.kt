package com.whish.contactsync

import android.content.Context
import android.provider.ContactsContract

data class DeviceContact(
    val displayName: String?,
    val phoneNumber: String
)

object ContactReader {

    fun readPhoneNumbers(context: Context): List<DeviceContact>
    {
        val deviceContacts = mutableListOf<DeviceContact>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext())
            {
                val displayName = it.getString(nameIndex)
                val rawNumber = it.getString(numberIndex)?.trim()

                if (!rawNumber.isNullOrBlank())
                {
                    deviceContacts.add(DeviceContact(displayName, rawNumber))
                }
            }
        }
        return deviceContacts.toList()
    }
}