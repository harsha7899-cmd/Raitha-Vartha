package com.example.raitha_vartha

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "agricultural_tips")
data class TipEntity(
    @PrimaryKey val id: String = "", 
    val title: String = "",
    val instruction: String = "",
    val category: String = "",
    val imageUrl: String = "",
    
    @get:PropertyName("isSuccessStory")
    @set:PropertyName("isSuccessStory")
    var isSuccessStory: Boolean = false,
    
    @get:PropertyName("isUserCrop")
    @set:PropertyName("isUserCrop")
    var isUserCrop: Boolean = false,

    val authorEmail: String = "",
    val isVerified: Boolean = false,
    val authorName: String = "",

    // New fields for Post Cards
    val isPostCard: Boolean = false,
    val isAdminApproved: Boolean = false,
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(), // Emails of users who liked
    val timestamp: Long = System.currentTimeMillis()
)

data class CommentEntity(
    val id: String = "",
    val authorEmail: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
