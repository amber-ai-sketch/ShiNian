package com.shinian.app.data.local

import androidx.room.*
import com.shinian.app.data.model.Note
import com.shinian.app.data.model.NoteType
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE type = :type ORDER BY createdAt DESC")
    fun getNotesByType(type: NoteType): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE isPending = 1 ORDER BY createdAt DESC")
    fun getPendingNotes(): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)
    
    @Update
    suspend fun updateNote(note: Note)
    
    @Delete
    suspend fun deleteNote(note: Note)
    
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)
    
    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int
}