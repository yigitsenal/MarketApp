package com.yigitsenal.marketapp.di

import com.yigitsenal.marketapp.data.network.MarketApiService
import com.yigitsenal.marketapp.data.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return RetrofitClient.getClient()
    }

    @Provides
    @Singleton
    fun provideMarketApiService(): MarketApiService {
        return RetrofitClient.getApiService()
    }
}
