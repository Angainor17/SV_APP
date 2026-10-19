package su.sv.commonarchitecture.data

import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Обертка для обращений к rest api. Если ваш запрос может возвращать особые ошибки, с особыми code и message в ошибке, то вам
 * нужно использовать эту обертку.
 * @param block вызов rest api метода
 * @return
 * - Успех: [R]
 * - Ошибка:
 * отреагировать логикой какой-то.
 * - Ошибка: [NetworkError] - разрыв соединения, не достучались до сервера и тд.
 * - Ошибка: [Exception] - любая другая ошибка
 */
inline fun <R> runCatchingHttpRequest(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Timber.e(e)
        if (e.isNetworkError) {
            Result.failure(NetworkError(e))
        } else {
            Result.failure(e)
        }
    }
}

val Throwable.isNetworkError: Boolean
    get() = if (this is IOException && this.cause is UnknownHostException) {
        true
    } else {
        when (this) {
            is NetworkError,
            is ConnectException,
            is UnknownHostException,
            is SocketTimeoutException -> true

            else -> false
        }
    }
