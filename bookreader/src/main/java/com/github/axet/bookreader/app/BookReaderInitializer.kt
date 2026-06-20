package com.github.axet.bookreader.app

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.net.toUri
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication
import timber.log.Timber

/**
 * Инициализатор BookReader.
 * Выносит логику инициализации из BookApplication для использования в любом Application классе.
 *
 * Использование:
 * ```
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         BookReaderInitializer.init(this)
 *     }
 * }
 * ```
 */
object BookReaderInitializer {

    private var zlib: ZLAndroidApplication? = null
    private var ttf: TTFManager? = null

    /**
     * Проверка, была ли выполнена инициализация
     */
    val isInitialized: Boolean
        get() = zlib != null

    /**
     * Получить менеджер TTF-шрифтов
     */
    fun getTTFManager(): TTFManager? = ttf

    /**
     * Инициализация BookReader.
     * Должна вызываться в Application.onCreate()
     *
     * @param context Application context
     */
    fun init(context: Context) {
        if (zlib != null) {
            Timber.d("BookReaderInitializer already initialized")
            return
        }

        Timber.d("BookReaderInitializer.init() started")

        // Инициализация ZLAndroidApplication (ядро FBReader)
        zlib = object : ZLAndroidApplication() {
            init {
                attachBaseContext(context)
                onCreate()
            }
        }

        // Инициализация менеджера шрифтов
        ttf = TTFManager(context)

        // Загрузка настроек папки шрифтов
        val shared = PreferenceManager.getDefaultSharedPreferences(context)
        val fonts: String = shared.getString(BookApplication.PREFERENCE_FONTS_FOLDER, "").orEmpty()
        if (fonts.isNotEmpty()) {
            try {
                val uri = fonts.toUri()
                Storage.takePersistableUriPermission(context, uri, Storage.SAF_RW)
                ttf?.setFolder(uri)
            } catch (e: Exception) {
                Timber.e(e, "Failed to set fonts folder")
            }
        }

        // Предзагрузка шрифтов
        ttf?.preloadFonts()

        Timber.d("BookReaderInitializer.init() completed")
    }

    /**
     * Получить SharedPreferences для настроек читалки
     */
    fun getSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
