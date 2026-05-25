package com.example.raitha_vartha

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val password: String = "",
    val profileImageUri: String? = null,
    val city: String = "Unknown",
    val village: String = "",
    val bio: String = "Farmer",
    val age: Int = 0,
    val yearsOfExperience: Int = 0,
    val idProofNumber: String = "", 
    val seedName: String = "", 
    
    @get:PropertyName("cropDocumentUri")
    @set:PropertyName("cropDocumentUri")
    var verificationDocumentUri: String? = null,
    
    @get:PropertyName("expert")
    @set:PropertyName("expert")
    var isExpert: Boolean = false,
    
    @get:PropertyName("pendingExpert")
    @set:PropertyName("pendingExpert")
    var isPendingExpert: Boolean = false,
    
    @get:PropertyName("admin")
    @set:PropertyName("admin")
    var isAdmin: Boolean = false
)
