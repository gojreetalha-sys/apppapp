package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestampIso: String,
    val triggerTimeMillis: Long,
    val ttsConfirmation: String,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
