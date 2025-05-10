package com.github.axet.bookreader.app

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.net.toUri
import com.github.axet.androidlibrary.app.MainApplication
import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication

open class BookApplication : MainApplication() {

    private lateinit var zlib: ZLAndroidApplication
    lateinit var ttf: TTFManager

    override fun onCreate() {
        super.onCreate()
        zlib = object : ZLAndroidApplication() {
            init {
                attachBaseContext(this@BookApplication)
                onCreate()
            }
        }
        ttf = TTFManager(this)
        val shared = PreferenceManager.getDefaultSharedPreferences(this)
        val fonts: String = shared.getString(PREFERENCE_FONTS_FOLDER, "").orEmpty()
        if (!fonts.isEmpty()) {
            val u = fonts.toUri()
            Storage.takePersistableUriPermission(this, u, Storage.SAF_RW)
            ttf.setFolder(u)
        }
        ttf.preloadFonts()
    }

    companion object {
        const val PREFERENCE_THEME: String = "theme"
        const val PREFERENCE_FONTFAMILY_FBREADER: String = "fontfamily_fb"
        const val PREFERENCE_FONTSIZE_FBREADER: String = "fontsize_fb"
        const val PREFERENCE_FONTSIZE_REFLOW: String = "fontsize_reflow"
        const val PREFERENCE_FONTSIZE_REFLOW_DEFAULT: Float = 0.8f
        const val PREFERENCE_LIBRARY_LAYOUT: String = "layout_"
        const val PREFERENCE_SCREENLOCK: String = "screen_lock"
        const val PREFERENCE_VOLUME_KEYS: String = "volume_keys"
        const val PREFERENCE_ROTATE: String = "rotate"
        const val PREFERENCE_VIEW_MODE: String = "view_mode"
        const val PREFERENCE_STORAGE: String = "storage_path"
        const val PREFERENCE_SORT: String = "sort"
        const val PREFERENCE_LANGUAGE: String = "tts_pref"
        const val PREFERENCE_IGNORE_EMBEDDED_FONTS: String = "ignore_embedded_fonts"
        const val PREFERENCE_FONTS_FOLDER: String = "fonts_folder"

        fun from(context: Context): BookApplication? {
            return MainApplication.from(context) as BookApplication?
        }

        fun getTheme(context: Context, light: Int, dark: Int): Int {
            return getTheme(context, PREFERENCE_THEME, light, dark)
        }
    }
}
