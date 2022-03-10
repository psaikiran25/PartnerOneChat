package com.bajaj.partneronechat.di

import com.bajaj.partneronechat.MainInteractor
import com.bajaj.partneronechat.MainRepository
import com.bajaj.partneronechat.WebServicesProvider
import com.bajaj.partneronechat.data.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Singleton
    @Provides
    fun provideWebServices(dataStoreManager: DataStoreManager): WebServicesProvider {
        return WebServicesProvider(dataStoreManager)
    }

    @Singleton
    @Provides
    fun providesRepository(webServicesProvider: WebServicesProvider): MainRepository {
        return MainRepository(webServicesProvider)
    }

    @Singleton
    @Provides
    fun providesInteractor(mainRepository: MainRepository): MainInteractor {
        return MainInteractor(mainRepository)
    }
}