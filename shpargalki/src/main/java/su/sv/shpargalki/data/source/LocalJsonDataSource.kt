package su.sv.shpargalki.data.source

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.sv.shpargalki.domain.model.DialecticsDocument
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * Локальный источник данных: читает gegel-dialektika.json из assets.
 * Используется в тестовом режиме, пока не готов API.
 */
class LocalJsonDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) : ShpargalkiDataSource {

    override suspend fun getDialectics(): Result<DialecticsDocument> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(ASSET_NAME).use { stream ->
                    gson.fromJson(
                        InputStreamReader(stream, Charsets.UTF_8),
                        DialecticsDocument::class.java,
                    )
                }
            }
        }

    private companion object {
        const val ASSET_NAME = "gegel-dialektika.json"
    }
}
