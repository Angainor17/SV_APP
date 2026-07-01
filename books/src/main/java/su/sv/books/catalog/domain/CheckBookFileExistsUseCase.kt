package su.sv.books.catalog.domain

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase для проверки существования файла книги по URI
 */
class CheckBookFileExistsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Проверить существование файла по URI
     * @param uriString URI файла в виде строки
     * @return true если файл существует и доступен, иначе false
     */
    fun execute(uriString: String?): Boolean {
        if (uriString == null) {
            Timber.d("Book file URI is null, file does not exist")
            return false
        }

        return try {
            val uri = uriString.toUri()
            val exists = checkUriExists(uri)
            Timber.d("Book file exists check: uri=$uriString, exists=$exists")
            exists
        } catch (e: Exception) {
            Timber.e(e, "Error checking book file existence: $uriString")
            false
        }
    }

    private fun checkUriExists(uri: Uri): Boolean {
        return try {
            // Самый надежный способ - попытка открыть InputStream
            // Это работает для всех типов URI и проверяет реальное существование файла
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                inputStream.close()
                Timber.d("File exists: $uri (InputStream opened successfully)")
                true
            } else {
                Timber.d("File does not exist: $uri (InputStream is null)")
                false
            }
        } catch (e: Exception) {
            Timber.w(e, "File does not exist or not accessible: $uri")
            false
        }
    }
}