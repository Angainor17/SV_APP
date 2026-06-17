package org.geometerplus.fbreader.formats

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filetypes.FileType

interface IFormatPluginCollection {
    fun getPlugin(file: ZLFile): FormatPlugin?
    fun getPlugin(fileType: FileType?): FormatPlugin?

    interface Holder {
        fun getCollection(): IFormatPluginCollection?
    }
}
