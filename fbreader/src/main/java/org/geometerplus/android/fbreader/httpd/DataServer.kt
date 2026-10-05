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
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Response.Status
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.book.CoverUtil
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.fbreader.formats.PluginImage
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLFileImageProxy
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.SliceInputStream
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DataServer(private val myService: DataService, port: Int) : NanoHTTPD(port) {

    override fun serve(
        uri: String,
        method: Method,
        headers: MutableMap<String, String>,
        params: MutableMap<String, String>,
        files: MutableMap<String, String>,
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
        headers: MutableMap<String, String>,
        params: MutableMap<String, String>,
        files: MutableMap<String, String>,
    ): Response {
        return try {
            val image = CoverUtil.getCover(
                DataUtil.fileFromEncodedPath(uri.substring(7)),
                PluginCollection.Instance(Paths.systemInfo(myService)),
            )
            when {
                image is ZLFileImageProxy -> {
                    image.synchronize()
                    val realImage = image.realImage ?: return notFound(uri)
                    var stream = realImage.inputStream() ?: return notFound(uri)
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
                    stream = realImage.inputStream() ?: return notFound(uri)
                    val res = Response(Status.OK, MimeType.IMAGE_PNG.toString(), stream)
                    res.setChunkedTransfer(true)
                    res.addHeader("X-Width", options.outWidth.toString())
                    res.addHeader("X-Height", options.outHeight.toString())
                    res
                }
                image is PluginImage -> {
                    if (image.isSynchronized) {
                        try {
                            val bitmap = (image.realImage as ZLBitmapImage).getBitmap()
                            val os = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, os)
                            val inputStream = ByteArrayInputStream(os.toByteArray())
                            val res = Response(Status.OK, MimeType.IMAGE_JPEG.toString(), inputStream)
                            res.addHeader("X-Width", bitmap.width.toString())
                            res.addHeader("X-Height", bitmap.height.toString())
                            res
                        } catch (t: Throwable) {
                            noContent(uri)
                        }
                    } else {
                        myService.ImageSynchronizer.synchronize(image, null)
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
        headers: MutableMap<String, String>,
        params: MutableMap<String, String>,
        files: MutableMap<String, String>,
    ): Response {
        var mime: String? = null
        for (mimeType in MimeType.TYPES_VIDEO) {
            val m = mimeType.toString()
            if (uri.startsWith("/$m/")) {
                mime = m
                break
            }
        }
        val foundMime = mime ?: return notFound(uri)
        return try {
            serveFile(DataUtil.fileFromEncodedPath(uri.substring(foundMime.length + 2)), foundMime, headers)
        } catch (e: Exception) {
            forbidden(uri, e)
        }
    }

    private fun serveFile(file: ZLFile, mime: String, headers: MutableMap<String, String>): Response {
        val baseStream = file.getInputStream()!!
        val fileLength = baseStream.available()
        val etag = '"' + Integer.toHexString(file.getPath().hashCode()) + '"'

        val range = headers["range"]
        val res: Response
        if (range == null || !range.startsWith(BYTES_PREFIX)) {
            if (etag == headers["if-none-match"]) {
                res = Response(Status.NOT_MODIFIED, mime, "")
            } else {
                res = Response(Status.OK, mime, baseStream)
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
                    if ("" != endString) {
                        end = endString.toInt()
                    }
                } catch (e: NumberFormatException) {
                }
            }
            if (start >= fileLength) {
                res = Response(
                    Status.RANGE_NOT_SATISFIABLE,
                    MimeType.TEXT_PLAIN.toString(),
                    "",
                )
                res.addHeader("ETag", etag)
                res.addHeader("Content-Range", "bytes 0-0/$fileLength")
            } else {
                if (end == -1 || end >= fileLength) {
                    end = fileLength - 1
                }
                res = Response(
                    Status.PARTIAL_CONTENT,
                    mime,
                    SliceInputStream(baseStream, start, end - start + 1),
                )
                res.addHeader("ETag", etag)
                res.addHeader("Content-Range", "bytes $start-$end/$fileLength")
            }
        }

        res.addHeader("Accept-Ranges", "bytes")
        return res
    }

    private fun notFound(uri: String): Response =
        Response(
            Status.NOT_FOUND,
            MimeType.TEXT_HTML.toString(),
            "<html><body><h1>Not found: $uri</h1></body></html>",
        )

    private fun noContent(uri: String): Response =
        Response(
            Status.NO_CONTENT,
            MimeType.TEXT_HTML.toString(),
            "<html><body><h1>No content: $uri</h1></body></html>",
        )

    private fun forbidden(uri: String, t: Throwable): Response {
        t.printStackTrace()
        return Response(
            Status.FORBIDDEN,
            MimeType.TEXT_HTML.toString(),
            "<html><body><h1>${t.message}</h1>\n($uri)</body></html>",
        )
    }

    companion object {
        private const val BYTES_PREFIX = "bytes="
    }
}
