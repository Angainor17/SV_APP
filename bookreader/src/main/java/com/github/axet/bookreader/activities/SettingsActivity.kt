package com.github.axet.bookreader.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import com.github.axet.androidlibrary.activities.AppCompatSettingsThemeActivity
import com.github.axet.androidlibrary.preferences.RotatePreferenceCompat
import com.github.axet.androidlibrary.preferences.StoragePathPreferenceCompat
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookApplication
import com.github.axet.bookreader.app.BookApplication.Companion.PREFERENCE_STORAGE
import com.github.axet.bookreader.app.Storage
import android.R as androidR

class SettingsActivity : AppCompatSettingsThemeActivity() {

    private val storage: Storage by lazy { Storage(this) }

    override fun getAppTheme(): Int {
        return BookApplication.getTheme(this, R.style.AppThemeLight, R.style.AppThemeDark)
    }

    override fun getAppThemeKey(): String? {
        return BookApplication.PREFERENCE_THEME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        changeStatusBarColor()

        RotatePreferenceCompat.onCreate(this, BookApplication.PREFERENCE_ROTATE)
        setupActionBar()
        if (savedInstanceState == null && intent.getParcelableExtra<Parcelable?>(SAVE_INSTANCE_STATE) == null) {
            showSettingsFragment(GeneralPreferenceFragment())
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        if (key == PREFERENCE_STORAGE) storage.migrateLocalStorageDialog(this)
    }

    class GeneralPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference(BookApplication.PREFERENCE_SCREENLOCK))
            bindPreferenceSummaryToValue(findPreference(BookApplication.PREFERENCE_THEME))
            bindPreferenceSummaryToValue(findPreference(BookApplication.PREFERENCE_VIEW_MODE))

            val s = findPreference(PREFERENCE_STORAGE) as StoragePathPreferenceCompat
            s.setStorage(Storage(context))
            s.setPermissionsDialog(this, Storage.PERMISSIONS_RW, RESULT_STORAGE)
            s.setStorageAccessFramework(this, RESULT_STORAGE)
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?
        ) {
            super.onViewCreated(view, savedInstanceState)

            /**
             * @Воронин
             * Фикс бага с отступом на экране настрое
             */
            val listContainer = view.findViewById<View?>(androidR.id.list_container)
            listContainer?.updatePadding(
                top = getToolBarHeight() + resources.getDimensionPixelSize(R.dimen.activity_vertical_margin),
            )
        }

        private fun getToolBarHeight(): Int {
            val attrs = intArrayOf(androidR.attr.actionBarSize)
            val ta = requireContext().obtainStyledAttributes(attrs)
            val toolBarHeight = ta.getDimensionPixelSize(0, -1)
            ta.recycle()
            return toolBarHeight
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        }

        override fun onResume() {
            super.onResume()
            val r = findPreference(BookApplication.PREFERENCE_ROTATE) as RotatePreferenceCompat
            r.onResume()
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            val s =
                findPreference(PREFERENCE_STORAGE) as StoragePathPreferenceCompat
            when (requestCode) {
                RESULT_STORAGE -> s.onRequestPermissionsResult(permissions, grantResults)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            val s =
                findPreference(PREFERENCE_STORAGE) as StoragePathPreferenceCompat
            when (requestCode) {
                RESULT_STORAGE -> s.onActivityResult(resultCode, data)
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == androidR.id.home) {
                val activity: Activity? = activity
                activity?.finish()
                startActivity(Intent(activity, MainActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val RESULT_STORAGE: Int = 1

        fun startActivity(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
