package su.sv.news.presentation.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Базовый PagingSource
 *
 * - В простом случае достаточно переопределить метод loadPage, который будет возвращать список объектов страницы.
 * - Если нужно больше кастомизации - можно переопределять уже реализованные в этом классе методы
 * - Если нужно изменить тип Key - нужно создавать свой PagingSource от [PagingSource]
 */
abstract class BasePagingSource<T : Any> : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val offset = params.key ?: DEFAULT_OFFSET
        val limit = params.loadSize

        return try {
            val result = loadPage(offset, limit).getOrThrow()
            LoadResult.Page(
                data = result,
                prevKey = if (offset == DEFAULT_OFFSET) null else offset - 1,
                nextKey = if (result.size < limit || result.isEmpty()) null else offset + result.size,
            )
        } catch (ex: Exception) {
            LoadResult.Error(ex)
        }
    }

    abstract suspend fun loadPage(offset: Int, limit: Int): Result<List<T>>

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition
    }

    companion object {
        const val DEFAULT_OFFSET = 0
    }
}
