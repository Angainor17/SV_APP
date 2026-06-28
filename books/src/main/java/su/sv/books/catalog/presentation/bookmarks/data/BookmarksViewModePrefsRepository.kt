package su.sv.books.catalog.presentation.bookmarks.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository для сохранения режима просмотра закладок
 */
@Singleton
class BookmarksViewModePrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Получить сохранённый режим просмотра
     */
    fun getViewMode(): String {
        return prefs.getString(KEY_VIEW_MODE, DEFAULT_VIEW_MODE) ?: DEFAULT_VIEW_MODE
    }

    /**
     * Сохранить режим просмотра
     */
    fun saveViewMode(mode: String) {
        prefs.edit().putString(KEY_VIEW_MODE, mode).apply()
    }

    companion object {
        private const val PREFS_NAME = "bookmarks_view_mode"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val DEFAULT_VIEW_MODE = "LIST"

        const val MODE_LIST = "LIST"
        const val MODE_BY_BOOK = "BY_BOOK"
    }
}