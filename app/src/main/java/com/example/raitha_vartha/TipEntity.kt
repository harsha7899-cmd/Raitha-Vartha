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
    
    @get:PropertyName("verified")
    @set:PropertyName("verified")
    var isVerified: Boolean = false,
    
    val authorName: String = "",

    @get:PropertyName("postCard")
    @set:PropertyName("postCard")
    var isPostCard: Boolean = false,
    
    @get:PropertyName("approvedByAdmin")
    @set:PropertyName("approvedByAdmin")
    var isAdminApproved: Boolean = false,
    
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class CommentEntity(
    val id: String = "",
    val authorEmail: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
