package com.yigitsenal.marketapp.data.model

data class PaginationInfo(
    val has_next: Boolean,
    val has_previous: Boolean,
    val current_page: Int,
    val total_pages: Int,
    val total_items: Int
)