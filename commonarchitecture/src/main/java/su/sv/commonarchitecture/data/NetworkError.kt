package su.sv.commonarchitecture.data

/**
 * Сетевая ошибка (разрыв соединения и т.п.) для domain слоя.
 */
class NetworkError(
    cause: Throwable,
) : Exception(cause)
