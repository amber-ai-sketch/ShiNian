package com.shinian.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String,
    val type: NoteType,
    val title: String? = null,
    val content: String,
    val audioPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isPending: Boolean = false
)

enum class NoteType {
    MEETING,    // 会议纪要
    IDEA,       // 灵感笔记
    TODO        // 待办事项
}