package com.github.axet.bookreader.services

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.github.axet.androidlibrary.app.AssetsDexLoader
import com.github.axet.androidlibrary.services.FileProvider
import com.github.axet.androidlibrary.services.StorageProvider
import org.geometerplus.zlibrary.core.image.ZLFileImage
import org.geometerplus.zlibrary.core.image.ZLImageData
import org.geometerplus.zlibrary.core.image.ZLImageManager
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream

/**
 * ContentProvider для предоставления изображений из книг.
 */
class ImagesProvider : StorageProvider() {

    companion object {
        const val EXT = "png"
        val TAG: String = ImagesProvider::class.java.simpleName

        /**
         * Возвращает экземпляр провайдера.
         */
        @JvmStatic
        fun getProvider(): ImagesProvider {
            return infos[ImagesProvider::class.java] as ImagesProvider
        }

        /**
         * Возвращает размер изображения.
         */
        @JvmStatic
        fun getImageSize(image: ZLFileImage): Long {
            return try {
                val aa = AssetsDexLoader.getPrivateField(image.javaClass, "myLengths")[image] as IntArray
                var c = 0
                for (a in aa)
                    c += a
                c.toLong()
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val f = find(uri) ?: return null
        val s = f.scheme
        if (s == ZLFileImage.SCHEME) {
            val image = ZLFileImage.byUrlPath(f.path) ?: return null

            var proj = projection
            if (proj == null)
                proj = FileProvider.COLUMNS

            val cursor = MatrixCursor(proj, 1)

            val cols = arrayOfNulls<String>(proj.size)
            val values = arrayOfNulls<Any>(proj.size)

            var i = 0
            for (col in proj) {
                when (col) {
                    OpenableColumns.DISPLAY_NAME -> {
                        cols[i] = OpenableColumns.DISPLAY_NAME
                        values[i++] = uri.lastPathSegment // содержит оригинальное имя
                    }
                    OpenableColumns.SIZE -> {
                        cols[i] = OpenableColumns.SIZE
                        values[i++] = getImageSize(image)
                    }
                }
            }

            val finalValues = java.util.Arrays.copyOf(values, i)
            cursor.addRow(finalValues)
            return cursor
        } else {
            return super.query(uri, projection, selection, selectionArgs, sortOrder)
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val f = find(uri) ?: return null

        freeUris()

        val image = ZLFileImage.byUrlPath(f.path) ?: throw FileNotFoundException()

        try {
            val imageData: ZLImageData = ZLImageManager.Instance().getImageData(image)
            val bm = (imageData as ZLAndroidImageData).fullSizeBitmap
            return openInputStream(object : InputStreamWriter() {
                override fun copy(os: OutputStream) {
                    try {
                        bm.compress(Bitmap.CompressFormat.PNG, 100, os)
                        bm.recycle()
                    } catch (e: Throwable) {
                        throw IOException(e)
                    }
                }

                override fun close() {}
            }, mode)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
        return AssetFileDescriptor(openFile(uri, mode), 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }
}
