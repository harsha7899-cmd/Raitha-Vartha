package com.example.raitha_vartha

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SuccessStoryDao {
    @Query("SELECT * FROM success_stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<SuccessStoryEntity>>

    @Insert
    suspend fun insertStory(story: SuccessStoryEntity)
}
