package su.sv.shpargalki.domain.model

/**
 * Одно понятие диалектики — узел вертикального древа.
 *
 * @param id уникальный идентификатор
 * @param order порядковый номер в последовательности выведения
 * @param term русское название понятия
 * @param german немецкое название (пустая строка, если отсутствует)
 * @param parent id родительского понятия (null у корня)
 * @param section раздел (bytie / suschnost / ponyatie)
 * @param subsection подраздел внутри раздела
 * @param popov пояснение по М. В. Попову
 * @param hegel исходное пояснение по Гегелю (самое полное)
 * @param svremya шпаргалка svremya.org
 * @param derivation как понятие проистекает из предыдущего
 */
data class DialecticsNode(
    val id: String,
    val order: Int,
    val term: String,
    val german: String,
    val parent: String?,
    val section: String,
    val subsection: String,
    val popov: String,
    val hegel: String,
    val svremya: String,
    val derivation: String,
)
