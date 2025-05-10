package com.github.axet.bookreader.activities

import android.app.Activity
import android.preference.PreferenceManager
import androidx.core.view.WindowInsetsControllerCompat
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.androidlibrary.R as AxetR

/**
 * Устанавливаю цвет статус бара
 */
fun Activity.changeStatusBarColor() {
    val shared = PreferenceManager.getDefaultSharedPreferences(this)
    val currentTheme = shared.getString(BookApplication.PREFERENCE_THEME, "").orEmpty()
    val isLightTheme = currentTheme == getString(AxetR.string.Theme_Light)

    // true - чёрная иконка
    // false - белая иконка
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
        isLightTheme
}
