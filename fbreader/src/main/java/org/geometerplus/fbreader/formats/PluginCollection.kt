package org.geometerplus.fbreader.formats

import android.os.Build
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filetypes.FileType
import org.geometerplus.zlibrary.core.filetypes.FileTypeCollection
import org.geometerplus.zlibrary.core.util.SystemInfo

class PluginCollection private constructor(systemInfo: SystemInfo) : IFormatPluginCollection {

    companion object {
        @Volatile
        private var instance: PluginCollection? = null

        init {
            System.loadLibrary("NativeFormats-v4")
        }

        @JvmStatic
        fun Instance(systemInfo: SystemInfo): PluginCollection {
            if (instance == null) {
                createInstance(systemInfo)
            }
            return instance!!
        }

        @Synchronized
        private fun createInstance(systemInfo: SystemInfo) {
            if (instance == null) {
                instance = PluginCollection(systemInfo)

                // This code cannot be moved to constructor
                // because nativePlugins() is a native method
                for (p in instance!!.nativePlugins(systemInfo)) {
                    instance!!.builtinPlugins.add(p)
                    System.err.println("native plugin: $p")
                }
            }
        }

        @JvmStatic
        fun deleteInstance() {
            instance = null
        }
    }

    private val builtinPlugins: MutableList<BuiltinFormatPlugin> = mutableListOf()
    private val externalPlugins: MutableList<ExternalFormatPlugin> = mutableListOf()

    init {
        if (Build.VERSION.SDK_INT >= 8) {
            externalPlugins.add(DjVuPlugin(systemInfo))
            externalPlugins.add(PDFPlugin(systemInfo))
            externalPlugins.add(ComicBookPlugin(systemInfo))
        }
    }

    override fun getPlugin(file: ZLFile): FormatPlugin? {
        val fileType = FileTypeCollection.Instance.typeForFile(file)
        val plugin = getPlugin(fileType)
        return if (plugin is ExternalFormatPlugin) {
            if (file == file.physicalFile) plugin else null
        } else {
            plugin
        }
    }

    override fun getPlugin(fileType: FileType?): FormatPlugin? {
        if (fileType == null) {
            return null
        }

        for (p in builtinPlugins) {
            if (fileType.Id.equals(p.supportedFileType(), ignoreCase = true)) {
                return p
            }
        }
        for (p in externalPlugins) {
            if (fileType.Id.equals(p.supportedFileType(), ignoreCase = true)) {
                return p
            }
        }
        return null
    }

    fun plugins(): List<FormatPlugin> {
        val all: MutableList<FormatPlugin> = ArrayList()
        all.addAll(builtinPlugins)
        all.addAll(externalPlugins)
        all.sortWith { p0, p1 ->
            val diff = p0.priority() - p1.priority()
            if (diff != 0) diff else p0.supportedFileType().compareTo(p1.supportedFileType())
        }
        return all
    }

    private external fun nativePlugins(systemInfo: SystemInfo): Array<NativeFormatPlugin>

    private external fun free()

    protected fun finalize() {
        free()
    }
}
