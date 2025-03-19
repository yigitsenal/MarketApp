package com.yigitsenal.marketapp.ui.viewmodel

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.repository.MarketRepository

class ProductPagingSource(
    private val repository: MarketRepository,
    private val query: String,
    private val sort: String?
) : PagingSource<Int, MarketItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MarketItem> {
        return try {
            val page = params.key ?: 1
            val response = repository.searchProducts(query, sort, page)

            LoadResult.Page(
                data = response.products ?: emptyList(),
                prevKey = if (response.pagination.has_previous) page - 1 else null,
                nextKey = if (response.pagination.has_next) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MarketItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}