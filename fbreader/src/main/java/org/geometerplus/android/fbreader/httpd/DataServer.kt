/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.httpd

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import fi.iki.elonen.NanoHTTPD
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.book.CoverUtil
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.formats.PluginImage
import org.geometerplus.zlibrary.core.image.ZLFileImageProxy
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.SliceInputStream
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

internal class DataServer(
    private val myService: DataService,
    port: Int
) : NanoHTTPD(port) {

    override fun serve(
        uri: String,
        method: Method,
        headers: Map<String, String>,
        params: Map<String, String>,
        files: Map<String, String>
    ): Response {
        return when {
            uri.startsWith("/cover/") -> serveCover(uri, method, headers, params, files)
            uri.startsWith("/video") -> serveVideo(uri, method, headers, params, files)
            else -> notFound(uri)
        }
    }

    private fun serveCover(
        uri: String,
        method: Method,
        headers: Map<String, String>,
        params: Map<String, String>,
        files: Map<String, String>
    ): Response {
        return try {
            val image = CoverUtil.getCover(
                DataUtil.fileFromEncodedPath(uri.substring(7)),
                PluginCollection.Instance(Paths.systemInfo(myService))
            )
            when (image) {
                is ZLFileImageProxy -> {
                    image.synchronize()
                    val realImage = image.realImage
                    if (realImage == null) return notFound(uri)
                    var stream = realImage.inputStream()
                    if (stream == null) return notFound(uri)
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    try {
                        BitmapFactory.decodeStream(stream, null, options)
                    } catch (e: Exception) {
                        return notFound(uri)
                    }
                    if (options.outWidth <= 0 || options.outHeight <= 0) {
                        return notFound(uri)
                    }
                    stream.close()
                    stream = realImage.inputStream()
                    if (stream == null) return notFound(uri)
                    val res = Response(Response.Status.OK, MimeType.IMAGE_PNG.toString(), stream)
                    res.addHeader("X-Width", options.outWidth.toString())
                    res.addHeader("X-Height", options.outHeight.toString())
                    res
                }
                is PluginImage -> {
                    if (image.isSynchronized) {
                        try {
                            val bitmap = (image.realImage as ZLBitmapImage).getBitmap()
                            val os = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, os)
                            val `is` = ByteArrayInputStream(os.toByteArray())
                            val res = Response(Response.Status.OK, MimeType.IMAGE_JPEG.toString(), `is`)
                            res.addHeader("X-Width", bitmap.width.toString())
                            res.addHeader("X-Height", bitmap.height.toString())
                            res
                        } catch (t: Throwable) {
                            noContent(uri)
                        }
                    } else {
                        myService.imageSynchronizer.synchronize(image, null)
                        noContent(uri)
                    }
                }
                else -> notFound(uri)
            }
        } catch (t: Throwable) {
            forbidden(uri, t)
        }
    }

    private fun serveVideo(
        uri: String,
        method: Method,
        headers: Map<String, String>,
        params: Map<String, String>,
        files: Map<String, String>
    ): Response {
        var mime: String? = null
        for (mimeType in MimeType.TYPES_VIDEO) {
            val m = mimeType.toString()
            if (uri.startsWith("/$m/")) {
                mime = m
                break
            }
        }
        if (mime == null) {
            return notFound(uri)
        }
        return try {
            serveFile(DataUtil.fileFromEncodedPath(uri.substring(mime.length + 2)), mime, headers)
        } catch (e: Exception) {
            forbidden(uri, e)
        }
    }

    @Throws(IOException::class)
    private fun serveFile(file: org.geometerplus.zlibrary.core.filesystem.ZLFile, mime: String, headers: Map<String, String>): Response {
        val res: Response
        val baseStream = file.getInputStream() ?: return notFound(file.path)
        val fileLength = baseStream.available()
        val etag = "\"${file.path.hashCode().toString(16)}\""

        val range = headers["range"]
        if (range == null || !range.startsWith(BYTES_PREFIX)) {
            if (etag == headers["if-none-match"]) {
                res = Response(Response.Status.NOT_MODIFIED, mime, "")
            } else {
                res = Response(Response.Status.OK, mime, baseStream)
                res.addHeader("ETag", etag)
            }
        } else {
            var start = 0
            var end = -1
            val bytes = range.substring(BYTES_PREFIX.length)
            val minus = bytes.indexOf('-')
            if (minus > 0) {
                try {
                    start = bytes.substring(0, minus).toInt()
                    val endString = bytes.substring(minus + 1).trim()
                    if (endString.isNotEmpty()) {
                        end = endString.toInt()
                    }
                } catch (e: NumberFormatException) {
                }
            }
            if (start >= fileLength) {
                res = Response(
                    Response.Status.RANGE_NOT_SATISFIABLE,
                    MimeType.TEXT_PLAIN.toString(),
                    ""
                )
                res.addHeader("ETag", etag)
                res.addHeader("Content-Range", "bytes 0-0/$fileLength")
            } else {
                if (end == -1 || end >= fileLength) {
                    end = fileLength - 1
                }
                res = Response(
                    Response.Status.PARTIAL_CONTENT,
                    mime,
                    SliceInputStream(baseStream, start, end - start + 1)
                )
                res.addHeader("ETag", etag)
                res.addHeader("Content-Range", "bytes $start-$end/$fileLength")
            }
        }

        res.addHeader("Accept-Ranges", "bytes")
        return res
    }

    private fun notFound(uri: String): Response = Response(
        Response.Status.NOT_FOUND,
        MimeType.TEXT_HTML.toString(),
        "<html><body><h1>Not found: $uri</h1></body></html>"
    )

    private fun noContent(uri: String): Response = Response(
        Response.Status.NO_CONTENT,
        MimeType.TEXT_HTML.toString(),
        "<html><body><h1>No content: $uri</h1></body></html>"
    )

    private fun forbidden(uri: String, t: Throwable): Response {
        t.printStackTrace()
        return Response(
            Response.Status.FORBIDDEN,
            MimeType.TEXT_HTML.toString(),
            "<html><body><h1>${t.message}</h1>\n($uri)</body></html>"
        )
    }

    companion object {
        private const val BYTES_PREFIX = "bytes="
    }
}
