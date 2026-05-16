package com.example.raitha_vartha

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TipEntity::class, UserEntity::class, SuccessStoryEntity::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tipDao(): TipDao
    abstract fun userDao(): UserDao
    abstract fun successStoryDao(): SuccessStoryDao
}
