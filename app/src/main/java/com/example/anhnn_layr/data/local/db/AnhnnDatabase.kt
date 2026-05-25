package com.example.anhnn_layr.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProjectDraftEntity::class], version = 1, exportSchema = false)
abstract class AnhnnDatabase : RoomDatabase() {
    abstract fun projectDraftDao(): ProjectDraftDao
}
