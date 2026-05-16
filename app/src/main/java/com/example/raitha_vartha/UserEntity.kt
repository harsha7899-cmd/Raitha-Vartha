package com.example.raitha_vartha

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val idProofNumber: String = "", // Aadhar or Voter ID
    val isExpert: Boolean = false
)
