package su.sv.shpargalki.domain.model

/**
 * Документ «древа выведения понятий» — структура gegel-dialektika.json.
 *
 * @param title название документа
 * @param sections словарь разделов (код раздела -> русское название)
 * @param nodes упорядоченный список понятий
 */
data class DialecticsDocument(
    val title: String,
    val sections: Map<String, String>,
    val nodes: List<DialecticsNode>,
)
