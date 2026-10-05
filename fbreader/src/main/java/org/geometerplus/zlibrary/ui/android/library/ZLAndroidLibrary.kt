/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.ui.android.library

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.util.DisplayMetrics
import org.geometerplus.android.util.DeviceType
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TreeSet

class ZLAndroidLibrary(private val myApplication: Application) : ZLibrary() {
    @JvmField
    val ShowStatusBarOption = ZLBooleanOption("LookNFeel", "ShowStatusBar", false)

    @JvmField
    val OldShowActionBarOption = ZLBooleanOption("LookNFeel", "ShowActionBar", true)

    @JvmField
    val ShowActionBarOption = ZLBooleanOption("LookNFeel", "ShowActionBarNew", false)

    @JvmField
    val EnableFullscreenModeOption = ZLBooleanOption("LookNFeel", "FullscreenMode", true)

    @JvmField
    val DisableButtonLightsOption =
        ZLBooleanOption("LookNFeel", "DisableButtonLights", !DeviceType.Instance().hasButtonLightsBug())

    @JvmField
    val BatteryLevelToTurnScreenOffOption =
        ZLIntegerRangeOption("LookNFeel", "BatteryLevelToTurnScreenOff", 0, 100, 50)

    @JvmField
    val DontTurnScreenOffDuringChargingOption =
        ZLBooleanOption("LookNFeel", "DontTurnScreenOffDuringCharging", true)

    @JvmField
    val ScreenBrightnessLevelOption = ZLIntegerRangeOption("LookNFeel", "ScreenBrightnessLevel", 0, 100, 0)

    private var myMetrics: DisplayMetrics? = null

    init {
        ShowStatusBarOption.setSpecialName("statusBar")
        OldShowActionBarOption.setSpecialName("actionBar")
        ShowActionBarOption.setSpecialName("actionBarNew")
        EnableFullscreenModeOption.setSpecialName("enableFullscreen")
        DisableButtonLightsOption.setSpecialName("disableButtonLights")
    }

    fun getAssets(): AssetManager = myApplication.assets

    override fun createResourceFile(path: String): ZLResourceFile = AndroidAssetsFile(path)

    override fun createResourceFile(parent: ZLResourceFile, name: String): ZLResourceFile =
        AndroidAssetsFile(parent as AndroidAssetsFile, name)

    override fun getVersionName(): String {
        try {
            val info: PackageInfo =
                myApplication.packageManager.getPackageInfo(myApplication.packageName, 0)
            return info.versionName ?: ""
        } catch (e: Exception) {
            return ""
        }
    }

    override fun getFullVersionName(): String {
        try {
            val info: PackageInfo =
                myApplication.packageManager.getPackageInfo(myApplication.packageName, 0)
            return info.versionName + " (" + info.versionCode + ")"
        } catch (e: Exception) {
            return ""
        }
    }

    override fun getCurrentTimeString(): String =
        DateFormat.getTimeFormat(myApplication.applicationContext).format(Date())

    private fun getMetrics(): DisplayMetrics {
        var metrics = myMetrics
        if (metrics == null) {
            metrics = myApplication.applicationContext.resources.displayMetrics
            myMetrics = metrics
        }
        return metrics
    }

    override fun getDisplayDPI(): Int {
        val metrics = getMetrics()
        return (160 * metrics.density).toInt()
    }

    override fun getWidthInPixels(): Int = getMetrics().widthPixels

    override fun getHeightInPixels(): Int = getMetrics().heightPixels

    override fun defaultLanguageCodes(): List<String> {
        val set = TreeSet<String>()
        set.add(Locale.getDefault().language)
        val manager = myApplication.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (manager != null) {
            var country0 = manager.simCountryIso
            if (country0 != null) {
                country0 = country0.lowercase(Locale.ROOT)
            }
            var country1 = manager.networkCountryIso
            if (country1 != null) {
                country1 = country1.lowercase(Locale.ROOT)
            }
            for (locale in Locale.getAvailableLocales()) {
                val country = locale.country.lowercase(Locale.ROOT)
                if (country.isNotEmpty() && (country == country0 || country == country1)) {
                    set.add(locale.language)
                }
            }
            if ("ru" == country0 || "ru" == country1) {
                set.add("ru")
            } else if ("by" == country0 || "by" == country1) {
                set.add("ru")
            } else if ("ua" == country0 || "ua" == country1) {
                set.add("ru")
            }
        }
        set.add("multi")
        return ArrayList(set)
    }

    override fun supportsAllOrientations(): Boolean = true

    private object StreamStatus {
        const val UNKNOWN = -1
        const val NULL = 0
        const val OK = 1
        const val EXCEPTION = 2
    }

    private inner class AndroidAssetsFile : ZLResourceFile {
        private val myParent: AndroidAssetsFile?
        private var myStreamStatus: Int = StreamStatus.UNKNOWN
        private var mySize: Long = -1

        constructor(parent: AndroidAssetsFile, name: String) : super(
            if (parent.getPath().length == 0) name else parent.getPath() + '/' + name
        ) {
            myParent = parent
        }

        constructor(path: String) : super(path) {
            if (path.length == 0) {
                myParent = null
            } else {
                val index = path.lastIndexOf('/')
                myParent = AndroidAssetsFile(if (index >= 0) path.substring(0, index) else "")
            }
        }

        protected override fun directoryEntries(): List<ZLFile> {
            try {
                val names = myApplication.assets.list(getPath())
                if (names != null && names.isNotEmpty()) {
                    val files = ArrayList<ZLFile>(names.size)
                    for (n in names) {
                        files.add(AndroidAssetsFile(this, n))
                    }
                    return files
                }
            } catch (e: IOException) {
            }
            return Collections.emptyList()
        }

        private fun streamStatus(): Int {
            if (myStreamStatus == StreamStatus.UNKNOWN) {
                try {
                    val stream = myApplication.assets.open(getPath())
                    if (stream == null) {
                        myStreamStatus = StreamStatus.NULL
                    } else {
                        stream.close()
                        myStreamStatus = StreamStatus.OK
                    }
                } catch (e: IOException) {
                    myStreamStatus = StreamStatus.EXCEPTION
                }
            }
            return myStreamStatus
        }

        override fun isDirectory(): Boolean = streamStatus() != StreamStatus.OK

        override fun exists(): Boolean {
            if (streamStatus() == StreamStatus.OK) {
                return true
            }
            val path = getPath()
            if ("" == path) {
                return true
            }
            // a hack: we store help files in fb2 format
            if (path.endsWith(".fb2")) {
                return false
            }
            try {
                val names = myApplication.assets.list(getPath())
                if (names != null && names.isNotEmpty()) {
                    // directory exists
                    return true
                }
            } catch (e: IOException) {
            }
            return false
        }

        override fun size(): Long {
            if (mySize == -1L) {
                mySize = sizeInternal()
            }
            return mySize
        }

        private fun sizeInternal(): Long {
            try {
                val descriptor: AssetFileDescriptor? = myApplication.assets.openFd(getPath())
                // for some files (archives, crt) descriptor cannot be opened
                if (descriptor == null) {
                    return sizeSlow()
                }
                val length = descriptor.length
                descriptor.close()
                return length
            } catch (e: IOException) {
                return sizeSlow()
            }
        }

        private fun sizeSlow(): Long {
            try {
                val stream = getInputStream()
                if (stream == null) {
                    return 0
                }
                var size = 0L
                val step = 1024L * 1024L
                while (true) {
                    // TODO: does skip work as expected for these files?
                    val offset = stream.skip(step)
                    size += offset
                    if (offset < step) {
                        break
                    }
                }
                return size
            } catch (e: IOException) {
                return 0
            }
        }

        @Throws(IOException::class)
        override fun getInputStream(): InputStream? = myApplication.assets.open(getPath())

        override fun getParent(): ZLFile? = myParent
    }
}
