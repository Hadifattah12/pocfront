package com.whish.contactsync.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.whish.contactsync.network.ContactPayloadDto

class ContactPayloadListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromPayloadList(value: List<ContactPayloadDto>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPayloadList(value: String): List<ContactPayloadDto> {
        val listType = object : TypeToken<List<ContactPayloadDto>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}

