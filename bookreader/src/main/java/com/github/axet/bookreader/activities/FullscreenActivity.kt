package com.github.axet.bookreader.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.github.axet.androidlibrary.activities.AppCompatFullscreenThemeActivity
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookApplication

open class FullscreenActivity : AppCompatFullscreenThemeActivity() {

    var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_bar_main)
        toolbar = findViewById<View?>(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun getAppTheme(): Int {
        return BookApplication.getTheme(
            this,
            R.style.AppThemeLight_NoActionBar,
            R.style.AppThemeDark_NoActionBar
        )
    }

    override fun getAppThemePopup(): Int {
        return BookApplication.getTheme(
            this,
            R.style.AppThemeLight_PopupOverlay,
            R.style.AppThemeDark_PopupOverlay
        )
    }

    @SuppressLint("InlinedApi", "RestrictedApi")
    override fun setFullscreen(b: Boolean) {
        super.setFullscreen(b)
        val fm = supportFragmentManager
        val ff = fm.fragments

        for (f in ff) {
            if (f is FullscreenListener) (f as FullscreenListener).onFullscreenChanged(b)
        }
    }

    override fun hideSystemUI() {
        super.hideSystemUI()
        setFitsSystemWindows(this, false)
    }

    override fun showSystemUI() {
        super.showSystemUI()
        setFitsSystemWindows(this, true)
    }

    @SuppressLint("RestrictedApi")
    override fun onUserInteraction() {
        super.onUserInteraction()
        val fm = supportFragmentManager
        val ff = fm.fragments

        for (f in ff) {
            if (f is FullscreenListener) (f as FullscreenListener).onUserInteraction()
        }
    }

    interface FullscreenListener {
        fun onFullscreenChanged(f: Boolean)

        fun onUserInteraction()
    }
}
