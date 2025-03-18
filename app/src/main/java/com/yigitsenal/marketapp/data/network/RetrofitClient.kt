package com.yigitsenal.marketapp.data.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var retrofit: Retrofit? = null
    
    fun getClient(): Retrofit {
        if (retrofit == null) {
            // Daha esnek bir Gson yapılandırması oluştur
            val gson = GsonBuilder()
                .setLenient() // JSON ayrıştırma sırasında daha esnek davran
                .create()
                
            retrofit = Retrofit.Builder()
                .baseUrl(MarketApiService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!
    }
    
    fun getApiService(): MarketApiService {
        return getClient().create(MarketApiService::class.java)
    }
}