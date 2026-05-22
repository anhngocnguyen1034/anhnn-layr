package com.example.anhnn_layr.di

import com.example.anhnn_layr.data.repository.RembgRepositoryImpl
import com.example.anhnn_layr.domain.repository.RembgRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRembgRepository(impl: RembgRepositoryImpl): RembgRepository
}
