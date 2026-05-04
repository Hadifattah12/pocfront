package com.example.contactsyncpoc.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingSyncChunkDao {
    @Query("SELECT * FROM pending_sync_chunk ORDER BY id ASC")
    suspend fun getPendingSyncChunks(): List<PendingSyncChunk>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSyncChunk(chunk: PendingSyncChunk): Long

    @Query("DELETE FROM pending_sync_chunk WHERE id = :id")
    suspend fun deletePendingSyncChunk(id: Long): Int
}

