package com.github.axet.bookreader.app

import com.github.axet.androidlibrary.app.MainApplication

/**
 * Application класс для BookReader.
 *
 * Для использования в собственном приложении наследуйте свой Application класс
 * от Application напрямую и вызовите BookReaderInitializer.init(this) в onCreate().
 *
 * Пример:
 * ```
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         BookReaderInitializer.init(this)
 *     }
 * }
 * ```
 */
open class BookApplication : MainApplication() {

    /**
     * Получить менеджер TTF-шрифтов
     */
    val ttf: TTFManager?
        get() = BookReaderInitializer.getTTFManager()

    override fun onCreate() {
        super.onCreate()
        BookReaderInitializer.init(this)
    }

    companion object {
        const val PREFERENCE_FONTFAMILY_FBREADER: String = "fontfamily_fb"
        const val PREFERENCE_FONTSIZE_FBREADER: String = "fontsize_fb"
        const val PREFERENCE_FONTSIZE_REFLOW: String = "fontsize_reflow"
        const val PREFERENCE_FONTSIZE_REFLOW_DEFAULT: Float = 0.8f
        const val PREFERENCE_SCREENLOCK: String = "screen_lock"
        const val PREFERENCE_VOLUME_KEYS: String = "volume_keys"
        const val PREFERENCE_ROTATE: String = "rotate"
        const val PREFERENCE_VIEW_MODE: String = "view_mode"
        const val PREFERENCE_STORAGE: String = "storage_path"
        const val PREFERENCE_LANGUAGE: String = "tts_pref"
        const val PREFERENCE_IGNORE_EMBEDDED_FONTS: String = "ignore_embedded_fonts"
        const val PREFERENCE_FONTS_FOLDER: String = "fonts_folder"
    }
}
