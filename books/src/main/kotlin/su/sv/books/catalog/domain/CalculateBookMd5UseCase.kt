package su.sv.books.catalog.domain

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject

/**
 * UseCase для вычисления MD5 хеша из URI файла книги
 *
 * Используется для связи между UiBook (fileUri) и BookmarkNote (bookId = MD5)
 */
class CalculateBookMd5UseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    /**
     * Вычисляет MD5 хеш из URI файла книги
     *
     * @param fileUri URI файла книги (content:// или file://)
     * @return Result с MD5 хешем (32 символа) или null при ошибке
     */
    suspend fun execute(fileUri: Uri): Result<String?> {
        return runCatching {
            withContext(dispatcherProvider.io) {
                calculateMd5(fileUri)
            }
        }
    }

    private fun calculateMd5(uri: Uri): String? {
        val digest = MessageDigest.getInstance("MD5")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            digest.digest()
                .joinToString("") { byte -> "%02x".format(byte) }
        } catch (e: Exception) {
            Timber.tag("voronin").e(e, "Failed to calculate MD5 for uri: $uri")
            null
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8192
    }
}