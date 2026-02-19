package com.shinian.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shinian.app.data.model.Note

@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "shinian_database"
    }
}