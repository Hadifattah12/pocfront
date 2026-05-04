package com.example.contactsyncpoc.data

import androidx.room.TypeConverter
import com.example.contactsyncpoc.network.ContactPayloadDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ContactPayloadListConverter {
    private val gson = Gson()
    private val contactPayloadListType = object : TypeToken<List<ContactPayloadDto>>() {}.type

    @TypeConverter
    fun fromContactPayloadList(value: List<ContactPayloadDto>): String {
        return gson.toJson(value, contactPayloadListType)
    }

    @TypeConverter
    fun toContactPayloadList(value: String): List<ContactPayloadDto> {
        return gson.fromJson(value, contactPayloadListType)
    }
}
