package su.sv.commonarchitecture.mock

import su.sv.commonarchitecture.BuildConfig

/**
 * Конфигурация мок-режима для сетевых запросов.
 *
 * Измените IS_MOCK_ENABLED на true для работы без интернета.
 * Моки позволяют разрабатывать и тестировать приложение offline.
 *
 * **Защита от утечки в релиз:** моки автоматически отключаются в release-сборках
 * через проверку [isMockAllowed]. Даже если IS_MOCK_ENABLED = true,
 * в release-сборке моки работать не будут.
 *
 * @since 2026-06-24
 */
object MockConfig {
    /**
     * Флаг включения мок-режима.
     * true = используются мок-данные, сетевые запросы не выполняются
     * false = выполняются реальные сетевые запросы
     *
     * **Действует только в debug-сборках.** В release-сборках моки
     * принудительно отключены независимо от значения этого флага.
     */
    const val IS_MOCK_ENABLED = false

    /**
     * Проверяет, разрешены ли моки в текущей сборке.
     *
     * Моки разрешены только в debug-сборках (BuildConfig.DEBUG = true).
     * В release-сборках моки принудительно отключены.
     *
     * @return true если моки разрешены (debug-сборка), false для release
     */
    fun isMockAllowed(): Boolean = BuildConfig.DEBUG
}
