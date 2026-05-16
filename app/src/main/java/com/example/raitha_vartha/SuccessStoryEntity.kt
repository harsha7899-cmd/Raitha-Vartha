package com.example.raitha_vartha

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "success_stories")
data class SuccessStoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val userName: String,
    val imageUri: String?,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)
