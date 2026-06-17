package org.geometerplus.fbreader

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Environment
import org.geometerplus.zlibrary.core.options.ZLStringListOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.SystemInfo
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

abstract class Paths {
    companion object {
        @JvmField
        val bookPathOption: ZLStringListOption = pathOption("BooksDirectory", defaultBookDirectory())

        @JvmField
        val fontPathOption: ZLStringListOption = pathOption("FontPathOption", cardDirectory() + "/Fonts")

        @JvmField
        val wallpaperPathOption: ZLStringListOption = pathOption("WallpapersDirectory", cardDirectory() + "/Wallpapers")

        @JvmField
        val downloadsDirectoryOption: ZLStringOption = ZLStringOption("Files", "DownloadsDirectory", "")

        private val tempDirectoryOption: ZLStringOption = ZLStringOption("Files", "TemporaryDirectory", "")

        init {
            if ("" == downloadsDirectoryOption.value) {
                downloadsDirectoryOption.value = mainBookDirectory()
            }
        }

        @JvmStatic
        @JvmName("TempDirectoryOption")
        fun tempDirectoryOption(context: Context?): ZLStringOption {
            if ("" == tempDirectoryOption.value) {
                tempDirectoryOption.value = internalTempDirectoryValue(context)
            }
            return tempDirectoryOption
        }

        private fun internalTempDirectoryValue(context: Context?): String {
            var value: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                value = getExternalCacheDirPath(context)
            }
            return if (value != null) value else mainBookDirectory() + "/.FBReader"
        }

        @TargetApi(Build.VERSION_CODES.FROYO)
        private fun getExternalCacheDirPath(context: Context?): String? {
            val d = context?.externalCacheDir
            if (d != null) {
                d.mkdirs()
                if (d.exists() && d.isDirectory) {
                    return d.path
                }
            }
            return null
        }

        private fun addDirToList(list: MutableList<String>, candidate: String?) {
            var candidate = candidate ?: return
            if (!candidate.startsWith("/")) return

            repeat(5) {
                while (candidate.endsWith("/")) {
                    candidate = candidate.substring(0, candidate.length - 1)
                }
                val f = File(candidate)
                try {
                    val canonical = f.canonicalPath
                    if (canonical == candidate) {
                        return@repeat
                    }
                    candidate = canonical
                } catch (t: Throwable) {
                    return
                }
            }
            while (candidate.endsWith("/")) {
                candidate = candidate.substring(0, candidate.length - 1)
            }
            if ("" != candidate && !list.contains(candidate) && File(candidate).canRead()) {
                list.add(candidate)
            }
        }

        @JvmStatic
        fun allCardDirectories(): List<String> {
            val dirs = mutableListOf<String>()
            dirs.add(cardDirectory())
            addDirToList(dirs, System.getenv("SECONDARY_STORAGE"))
            /*
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(FileReader("/system/etc/vold.fstab"))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val hashIndex = line!!.indexOf("#")
                    if (hashIndex >= 0) {
                        line = line!!.substring(0, hashIndex)
                    }
                    val parts = line!!.split("\\s+".toRegex())
                    if (parts.size >= 5) {
                        addDirToList(dirs, parts[2])
                    }
                }
            } catch (e: Throwable) {
            } finally {
                try {
                    reader?.close()
                } catch (t: Throwable) {
                }
            }
            */
            return dirs
        }

        @JvmStatic
        fun cardDirectory(): String {
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
                return Environment.getExternalStorageDirectory().path
            }

            val dirNames = mutableListOf<String>()
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(FileReader("/proc/self/mounts"))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("\\s+".toRegex())
                    if (parts.size >= 4 &&
                        parts[2].lowercase().indexOf("fat") >= 0 &&
                        parts[3].indexOf("rw") >= 0
                    ) {
                        val fsDir = File(parts[1])
                        if (fsDir.isDirectory && fsDir.canWrite()) {
                            dirNames.add(fsDir.path)
                        }
                    }
                }
            } catch (e: Throwable) {
            } finally {
                try {
                    reader?.close()
                } catch (t: Throwable) {
                }
            }

            for (dir in dirNames) {
                if (dir.lowercase().indexOf("media") > 0) {
                    return dir
                }
            }
            if (dirNames.isNotEmpty()) {
                return dirNames[0]
            }

            return Environment.getExternalStorageDirectory().path
        }

        private fun defaultBookDirectory(): String = cardDirectory() + "/Books"

        private fun pathOption(key: String, defaultDirectory: String): ZLStringListOption {
            val option = ZLStringListOption("Files", key, emptyList(), "\n")
            if (option.value.isEmpty()) {
                option.value = listOf(defaultDirectory)
            }
            return option
        }

        @JvmStatic
        fun bookPath(): List<String> {
            val path = ArrayList(bookPathOption.value)
            val downloadsDirectory = downloadsDirectoryOption.value
            if ("" != downloadsDirectory && !path.contains(downloadsDirectory)) {
                path.add(downloadsDirectory)
            }
            return path
        }

        @JvmStatic
        fun mainBookDirectory(): String {
            val bookPath = bookPathOption.value
            return if (bookPath.isEmpty()) defaultBookDirectory() else bookPath[0]
        }

        @JvmStatic
        fun systemInfo(context: Context): SystemInfo {
            val appContext = context.applicationContext
            return object : SystemInfo {
                override fun tempDirectory(): String {
                    val value = tempDirectoryOption.value
                    if ("" != value) {
                        return value
                    }
                    return internalTempDirectoryValue(appContext)
                }

                override fun networkCacheDirectory(): String = tempDirectory() + "/cache"
            }
        }

        @JvmStatic
        fun systemShareDirectory(): String = "/system/usr/share/FBReader"
    }
}
