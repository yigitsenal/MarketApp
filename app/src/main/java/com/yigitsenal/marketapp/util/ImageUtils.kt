package com.yigitsenal.marketapp.util

import android.content.Context
import android.util.Log
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yigitsenal.marketapp.data.network.MarketApiService
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ImageUtils {
    
    fun createImageRequest(context: Context, imageName: String, size: String = "md"): ImageRequest {
        // Extract just the filename from the image field if it contains a URL-like structure
        val actualFileName = extractFilename(imageName)
        Log.d("ImageUtils", "Original image name: $imageName")
        Log.d("ImageUtils", "Extracted filename: $actualFileName")
        
        // Encode the filename properly
        val encodedImageName = URLEncoder.encode(actualFileName, StandardCharsets.UTF_8.toString())
        
        // Create the URL with the proper format
        val imageUrl = "http://10.0.2.2:8000/image.php?file=$encodedImageName&size=$size"
        
        Log.d("ImageUtils", "Loading image from URL: $imageUrl")
        
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCachePolicy(CachePolicy.DISABLED) // Disable memory cache for debugging
            .diskCachePolicy(CachePolicy.DISABLED) // Disable disk cache for debugging
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
    
    /**
     * Extracts the actual filename from a potentially URL-like string
     * For example, if the input is "/image.php?file=product-name.jpg&size=md",
     * this function will return "product-name.jpg"
     */
    private fun extractFilename(imagePath: String): String {
        // If the image path contains a URL query parameter "file="
        return if (imagePath.contains("file=")) {
            val fileParam = imagePath.substringAfter("file=").substringBefore("&")
            Log.d("ImageUtils", "Found file parameter: $fileParam")
            fileParam
        } else {
            // Otherwise just return the original string
            imagePath
        }
    }
}
