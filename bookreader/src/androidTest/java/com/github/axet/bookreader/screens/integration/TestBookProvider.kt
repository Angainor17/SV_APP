package com.github.axet.bookreader.screens.integration

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Провайдер тестовой книги.
 *
 * По умолчанию использует EPUB из assets.
 * Может использовать книги из Downloads если они доступны.
 */
object TestBookProvider {

    private const val TAG = "TestBookProvider"

    // Книги в assets
    private const val ASSET_BOOK_EPUB = "test_book.epub"

    // Имена во внутреннем хранилище
    private const val INTERNAL_BOOK_EPUB = "test_book.epub"

    /**
     * Получить тестовую книгу.
     */
    fun getTestBook(): TestBook {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Вариант 1: EPUB из assets
        val epubBook = copyFromAssets(context, ASSET_BOOK_EPUB, INTERNAL_BOOK_EPUB)
        if (epubBook != null) {
            Log.d(TAG, "Using EPUB from assets: ${epubBook.length()} bytes")
            return TestBook(
                uri = Uri.fromFile(epubBook),
                title = "Moby Dick (EPUB)",
                author = "Herman Melville",
                format = BookFormat.EPUB
            )
        }

        Log.w(TAG, "No test book available")
        return TestBook(Uri.EMPTY, "Нет книги", "", BookFormat.UNKNOWN)
    }

    /**
     * Получить книгу определённого формата.
     */
    fun getTestBook(format: BookFormat): TestBook {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        when (format) {
            BookFormat.EPUB -> {
                val book = copyFromAssets(context, ASSET_BOOK_EPUB, INTERNAL_BOOK_EPUB)
                if (book != null) {
                    return TestBook(Uri.fromFile(book), "Moby Dick", "Herman Melville", BookFormat.EPUB)
                }
            }
            BookFormat.PDF -> {
                // PDF берём из Downloads если есть
                val pdfFile = findInDownloads(context, ".pdf")
                if (pdfFile != null) {
                    val internal = copyToInternal(context, pdfFile, "test_book.pdf")
                    if (internal != null) {
                        return TestBook(Uri.fromFile(internal), "PDF Book", "", BookFormat.PDF)
                    }
                }
            }
            BookFormat.FB2 -> {
                val fb2File = findInDownloads(context, ".fb2")
                if (fb2File != null) {
                    val internal = copyToInternal(context, fb2File, "test_book.fb2")
                    if (internal != null) {
                        return TestBook(Uri.fromFile(internal), "FB2 Book", "", BookFormat.FB2)
                    }
                }
            }
            BookFormat.UNKNOWN -> {}
        }

        return TestBook(Uri.EMPTY, "Нет книги", "", BookFormat.UNKNOWN)
    }

    /**
     * Проверить доступность книги.
     */
    fun hasTestBook(): Boolean = true

    private fun copyFromAssets(context: Context, assetName: String, internalName: String): File? {
        val targetFile = File(context.filesDir, internalName)

        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile
        }

        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied from assets: $assetName")
            return targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy from assets: ${e.message}")
            return null
        }
    }

    private fun findInDownloads(context: Context, extension: String): File? {
        val paths = listOf("/sdcard/Download", "/storage/emulated/0/Download")
        for (path in paths) {
            val dir = File(path)
            if (dir.exists()) {
                val files = dir.listFiles()?.filter { it.name.endsWith(extension) }
                if (!files.isNullOrEmpty()) return files.first()
            }
        }
        return null
    }

    private fun copyToInternal(context: Context, source: File, name: String): File? {
        val target = File(context.filesDir, name)
        try {
            source.inputStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            return target
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy: ${e.message}")
            return null
        }
    }
}

enum class BookFormat {
    EPUB, PDF, FB2, UNKNOWN
}

data class TestBook(
    val uri: Uri,
    val title: String,
    val author: String,
    val format: BookFormat = BookFormat.EPUB
) {
    fun isAvailable(): Boolean = uri != Uri.EMPTY
}