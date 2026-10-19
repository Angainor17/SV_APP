package su.sv.shpargalki.data.source

import su.sv.shpargalki.domain.model.DialecticsDocument
import javax.inject.Inject

/**
 * Заглушка будущего API-источника.
 * Когда данные переедут на сервер, здесь будет Retrofit-вызов.
 */
class ApiDataSource @Inject constructor() : ShpargalkiDataSource {

    override suspend fun getDialectics(): Result<DialecticsDocument> =
        Result.failure(NotImplementedError("API-источник шпаргалок ещё не реализован"))
}
