package com.example.contactsyncpoc.data

import androidx.room.*

@Dao
abstract class ContactDao {
    @Query("SELECT * FROM contact_snapshot")
    abstract suspend fun getAllContacts(): List<ContactSnapshot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(contacts: List<ContactSnapshot>)

    @Query("DELETE FROM contact_snapshot")
    abstract suspend fun deleteAll(): Int

    @Transaction
    open suspend fun updateSnapshot(contacts: List<ContactSnapshot>) {
        deleteAll()
        insertAll(contacts)
    }
}
