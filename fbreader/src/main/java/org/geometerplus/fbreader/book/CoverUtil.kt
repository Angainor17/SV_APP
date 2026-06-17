package org.geometerplus.fbreader.book

import org.geometerplus.fbreader.formats.IFormatPluginCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLImage

import java.lang.ref.WeakReference
import java.util.WeakHashMap

object CoverUtil {
    private val NULL_IMAGE: WeakReference<ZLImage?> = WeakReference(null)
    private val covers: WeakHashMap<ZLFile, WeakReference<ZLImage?>> = WeakHashMap()

    @JvmStatic
    fun getCover(book: AbstractBook?, collection: IFormatPluginCollection): ZLImage? {
        if (book == null) {
            return null
        }
        synchronized(book) {
            return getCover(ZLFile.createFileByPath(book.getPath()), collection)
        }
    }

    @JvmStatic
    fun getCover(file: ZLFile?, collection: IFormatPluginCollection): ZLImage? {
        if (file == null) return null

        val cover = covers[file]
        if (cover === NULL_IMAGE) {
            return null
        } else if (cover != null) {
            val image = cover.get()
            if (image != null) {
                return image
            }
        }
        var image: ZLImage? = null
        try {
            image = collection.getPlugin(file)?.readCover(file)
        } catch (e: Exception) {
            // ignore
        }
        covers[file] = if (image != null) WeakReference(image) else NULL_IMAGE
        return image
    }
}
