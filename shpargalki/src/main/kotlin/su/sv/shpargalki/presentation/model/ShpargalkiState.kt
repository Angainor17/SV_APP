package su.sv.shpargalki.presentation.model

import su.sv.shpargalki.domain.model.DialecticsDocument

/**
 * Состояние экрана «Шпаргалки».
 */
sealed class ShpargalkiState {

    /** Загрузка данных */
    object Loading : ShpargalkiState()

    /** Данные загружены */
    data class Content(val document: DialecticsDocument) : ShpargalkiState()

    /** Ошибка загрузки */
    data class Error(val message: String) : ShpargalkiState()
}
