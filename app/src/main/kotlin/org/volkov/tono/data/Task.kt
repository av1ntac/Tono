package org.volkov.tono.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val dayKey: String,
    val position: Int,
    val text: String
)
