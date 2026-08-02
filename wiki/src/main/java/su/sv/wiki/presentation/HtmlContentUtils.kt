package su.sv.wiki.presentation

private val numericDecimalEntity = Regex("&#(\\d+);")
private val numericHexEntity = Regex("&#[xX]([0-9a-fA-F]+);")

/**
 * Декодирует HTML-сущности (numeric, hex и основные именованные) в реальные символы.
 * MediaWiki API отдаёт контент как HTML — без декодирования сущности вроде "&#160;"
 * показываются на экране как есть, а не как невидимый неразрывный пробел.
 *
 * "&amp;" декодируется последним, чтобы не задвоить распознавание (иначе "&amp;#160;"
 * превратилось бы сначала в "&#160;", а затем ошибочно ещё раз обработалось бы как numeric entity).
 */
fun String.decodeHtmlEntities(): String {
    return this
        .replace(numericDecimalEntity) { it.groupValues[1].toInt().toChar().toString() }
        .replace(numericHexEntity) { it.groupValues[1].toInt(16).toChar().toString() }
        .replace("&nbsp;", " ")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&hellip;", "…")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}
