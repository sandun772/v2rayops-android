package com.v2rayops.client.di

import com.v2rayops.client.data.repository.AuthRepositoryImpl
import com.v2rayops.client.data.repository.ServerRepositoryImpl
import com.v2rayops.client.data.repository.SettingsRepositoryImpl
import com.v2rayops.client.data.repository.SubscriptionRepositoryImpl
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SettingsRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
