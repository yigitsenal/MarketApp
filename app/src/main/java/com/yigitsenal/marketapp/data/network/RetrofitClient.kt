package com.yigitsenal.marketapp.data.network

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var retrofit: Retrofit? = null
    
    private class DoubleTypeAdapter : TypeAdapter<Double>() {
        override fun write(out: JsonWriter, value: Double?) {
            if (value == null) {
                out.nullValue()
            } else {
                out.value(value)
            }
        }

        override fun read(reader: JsonReader): Double {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return 0.0
            }
            if (reader.peek() == JsonToken.STRING) {
                val stringValue = reader.nextString()
                if (stringValue.isEmpty()) {
                    return 0.0
                }
                return stringValue.toDoubleOrNull() ?: 0.0
            }
            return reader.nextDouble()
        }
    }

    private class IntTypeAdapter : TypeAdapter<Int>() {
        override fun write(out: JsonWriter, value: Int?) {
            if (value == null) {
                out.nullValue()
            } else {
                out.value(value)
            }
        }

        override fun read(reader: JsonReader): Int {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return 0
            }
            if (reader.peek() == JsonToken.STRING) {
                val stringValue = reader.nextString()
                if (stringValue.isEmpty()) {
                    return 0
                }
                return stringValue.toIntOrNull() ?: 0
            }
            return reader.nextInt()
        }
    }
    
    fun getClient(): Retrofit {
        if (retrofit == null) {
            val gson = GsonBuilder()
                .setLenient()
                .registerTypeAdapter(Double::class.java, DoubleTypeAdapter())
                .registerTypeAdapter(Int::class.java, IntTypeAdapter())
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