package su.sv.shpargalki.data.source

import su.sv.shpargalki.domain.model.DialecticsDocument

/**
 * Источник данных для шпаргалок.
 *
 * Абстракция позволяет переключаться между локальным JSON и API без изменения
 * вышележащих слоёв (repository/usecase/presentation).
 */
interface ShpargalkiDataSource {

    /**
     * Получить полный документ диалектики (все понятия).
     */
    suspend fun getDialectics(): Result<DialecticsDocument>
}
