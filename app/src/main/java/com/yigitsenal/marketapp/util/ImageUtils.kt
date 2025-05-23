package com.yigitsenal.marketapp.util

import android.content.Context
import android.util.Log
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ImageUtils {
    
    fun createImageRequest(context: Context, imageName: String, size: String = Constants.ImageSizes.MEDIUM): ImageRequest {
        // URL benzeri bir yapı içeriyorsa resim alanından sadece dosya adını çıkar
        val actualFileName = extractFilename(imageName)
        Log.d("ImageUtils", "Original image name: $imageName")
        Log.d("ImageUtils", "Extracted filename: $actualFileName")
        

        val encodedImageName = URLEncoder.encode(actualFileName, StandardCharsets.UTF_8.toString())

        // URL'yi uygun biçimde oluşturma
        val imageUrl = "${Constants.ApiEndpoints.IMAGE}?file=$encodedImageName&size=$size"
        
        Log.d("ImageUtils", "Loading image from URL: $imageUrl")
        
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCachePolicy(CachePolicy.DISABLED) // Hata ayıklama için bellek önbelleğini devre dışı bırak
            .diskCachePolicy(CachePolicy.DISABLED) // Hata ayıklama için disk önbelleğini devre dışı bırak
            .crossfade(true)
            .listener(
                onStart = { request ->
                    Log.d("ImageUtils", "Started loading: ${request.data}")
                },
                onSuccess = { _, _ ->
                    Log.d("ImageUtils", "Successfully loaded: $imageUrl")
                },
                onError = { _, error ->
                    Log.e("ImageUtils", "Error loading image: $imageUrl", error.throwable)
                }
            )
            .build()
    }
    

    private fun extractFilename(imagePath: String): String {

        return if (imagePath.contains("file=")) {
            val fileParam = imagePath.substringAfter("file=").substringBefore("&")
            Log.d("ImageUtils", "Found file parameter: $fileParam")
            fileParam
        } else {

            imagePath
        }
    }
}
