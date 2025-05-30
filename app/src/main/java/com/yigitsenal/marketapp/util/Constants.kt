package com.yigitsenal.marketapp.util

object Constants {
    const val API_BASE_URL = "http://192.168.1.7:8000"
    
    object ApiEndpoints {
        const val SEARCH = "$API_BASE_URL/api.php"
        const val PRODUCT_DETAILS = "$API_BASE_URL/product.php"
        const val IMAGE = "$API_BASE_URL/image.php"
    }
    
    object ImageSizes {
        const val SMALL = "sm"
        const val MEDIUM = "md"
        const val LARGE = "lg"
    }
} 