package su.sv.main.badge

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер для управления состоянием бейджей "new" в навигации
 */
@Singleton
class BadgeManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Проверить, нужно ли показывать бейдж "new" для Wiki
     */
    fun shouldShowWikiBadge(): Boolean {
        return !prefs.getBoolean(KEY_WIKI_VISITED, false)
    }

    /**
     * Отметить Wiki как посещённый (скрыть бейдж)
     */
    fun markWikiAsVisited() {
        prefs.edit {
            putBoolean(KEY_WIKI_VISITED, true)
        }
    }

    companion object {
        private const val PREFS_NAME = "navigation_badges"
        private const val KEY_WIKI_VISITED = "wiki_visited"
    }
}
