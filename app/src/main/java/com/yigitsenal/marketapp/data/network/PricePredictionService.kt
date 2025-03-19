package com.yigitsenal.marketapp.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface PricePredictionApiService {
    @Headers(
        "Content-Type: application/json"
    )
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun predictPrice(
        @Body request: PredictionRequest,
        @Query("key") apiKey: String
    ): PredictionResponse

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
        // API anahtarı
        const val API_KEY = "AIzaSyAZLTdydqZPNQR8AYAiJ7SPjGKj5LsHfKE" // Gerçek kullanımda uygulamaya ait bir API anahtarı kullanılmalıdır

        fun create(): PricePredictionApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(PricePredictionApiService::class.java)
        }
        
        fun buildRequestForPricePrediction(productName: String, priceHistory: List<PriceHistoryEntry>): PredictionRequest {
            val promptText = buildPromptText(productName, priceHistory)
            return PredictionRequest(
                contents = listOf(
                    RequestContent(
                        parts = listOf(
                            Part(text = promptText)
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.2,
                    maxOutputTokens = 300
                ),
                safetySettings = listOf(
                    SafetySetting(
                        category = "HARM_CATEGORY_HARASSMENT",
                        threshold = "BLOCK_NONE"
                    )
                )
            )
        }
        
        private fun buildPromptText(productName: String, priceHistory: List<PriceHistoryEntry>): String {
            val historyText = priceHistory.joinToString("\n") { "${it.date}: ${it.price} TL" }
            
            return """
                Ürün adı: $productName
                
                Aşağıdaki geçmiş fiyat verileri kullanarak, bu ürünün gelecekteki fiyat tahminini yap. 
                Sadece gelecek 30, 60 ve 90 gün için tahmini fiyatları ver. Nedenleriyle birlikte açıkla.
                
                Geçmiş fiyat verileri:
                $historyText
                
                Lütfen sadece şu formatta yanıt ver:
                30 Gün: [tahmin] TL
                60 Gün: [tahmin] TL 
                90 Gün: [tahmin] TL
                
                Tahminin nedenleri:
                [kısa analiz]
            """.trimIndent()
        }
    }
}

data class PriceHistoryEntry(
    val date: String,
    val price: Double
)

data class PredictionRequest(
    val contents: List<RequestContent>,
    val generationConfig: GenerationConfig,
    val safetySettings: List<SafetySetting>
)

data class RequestContent(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Double,
    val maxOutputTokens: Int
)

data class SafetySetting(
    val category: String,
    val threshold: String
)

data class PredictionResponse(
    val candidates: List<Candidate>?,
    val promptFeedback: PromptFeedback?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?
)

data class Content(
    val parts: List<Part>?,
    val role: String?
)

data class PromptFeedback(
    val safetyRatings: List<SafetyRating>?
)

data class SafetyRating(
    val category: String?,
    val probability: String?
)

data class PricePredictionResult(
    val prediction30Days: Double?,
    val prediction60Days: Double?,
    val prediction90Days: Double?,
    val analysis: String?,
    val errorMessage: String? = null
) 