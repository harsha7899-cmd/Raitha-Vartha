package com.example.raitha_vartha

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TipDao {
    @Query("SELECT * FROM agricultural_tips")
    fun getAllTips(): Flow<List<TipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: TipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTips(tips: List<TipEntity>)

    @Query("UPDATE agricultural_tips SET isUserCrop = :isMine WHERE id = :tipId")
    suspend fun toggleMyCrop(tipId: String, isMine: Boolean)

    @Query("DELETE FROM agricultural_tips")
    suspend fun deleteAllTips()
}
