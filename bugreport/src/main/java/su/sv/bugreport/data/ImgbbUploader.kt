package su.sv.bugreport.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис загрузки изображений на Imgbb
 */
@Singleton
class ImgbbUploader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Загружает изображение на Imgbb и возвращает ссылку
     *
     * @param uri URI изображения
     * @param timestamp ID баг-репорта для именования
     * @param index индекс скриншота
     * @return URL загруженного изображения или null при ошибке
     */
    suspend fun uploadImage(
        uri: Uri,
        timestamp: Long,
        index: Int,
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Копируем Uri во временный файл
            val tempFile = copyUriToTempFile(uri, timestamp, index)

            // Загружаем на Imgbb
            val imageUrl = uploadToImgbb(tempFile)

            // Удаляем временный файл
            tempFile.delete()

            imageUrl
        } catch (e: Exception) {
            Timber.tag("voronin").e(e, "Failed to upload image to Imgbb")
            null
        }
    }

    /**
     * Копирует Uri во временный файл
     */
    private fun copyUriToTempFile(uri: Uri, timestamp: Long, index: Int): File {
        val cacheDir = File(context.cacheDir, "bug_reports")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val fileName = "screenshot_${timestamp}_$index.jpg"
        val outputFile = File(cacheDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalArgumentException("Cannot open input stream for uri: $uri")

        return outputFile
    }

    /**
     * Загружает файл на Imgbb через API
     */
    private fun uploadToImgbb(file: File): String? {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val url = "https://api.imgbb.com/1/upload?key=$API_KEY"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.tag("voronin").e("Imgbb upload failed: ${response.code}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            return parseImageUrl(responseBody)

        } catch (e: Exception) {
            Timber.tag("voronin").e(e, "Failed to upload to Imgbb")
            return null
        }
    }

    /**
     * Парсит ответ от Imgbb API и извлекает URL
     */
    private fun parseImageUrl(jsonResponse: String): String? {
        return try {
            val json = JSONObject(jsonResponse)
            val data = json.getJSONObject("data")
            val url = data.getString("url")
            Timber.tag("voronin").d("Image uploaded to Imgbb: $url")
            url
        } catch (e: Exception) {
            Timber.tag("voronin").e(e, "Failed to parse Imgbb response")
            null
        }
    }

    companion object {
        // API ключ для загрузки изображений на Imgbb
        // Получен на https://imgbb.com/
        const val API_KEY = "b4d9b1eb07f78d1d5cad70253cd29b03"
    }
}