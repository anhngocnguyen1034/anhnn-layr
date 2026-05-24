package com.example.anhnn_layr.di

import android.content.Context
import androidx.room.Room
import com.example.anhnn_layr.data.local.db.AnhnnDatabase
import com.example.anhnn_layr.data.local.db.ProjectDraftDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AnhnnDatabase =
        Room.databaseBuilder(ctx, AnhnnDatabase::class.java, "anhnn.db").build()

    @Provides
    fun provideProjectDraftDao(db: AnhnnDatabase): ProjectDraftDao = db.projectDraftDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
