package su.sv.commonui.theme

/**
 * Атрибуты цвета темы для редактирования в отладочном экране.
 *
 * @param attributeName имя атрибута (соответствует имени в ColorScheme)
 * @param description описание - за что отвечает цвет
 * @param group группа атрибутов для организации в UI
 */
enum class CustomColorAttribute(
    val attributeName: String,
    val description: String,
    val group: ColorGroup,
) {
    // ============================================================
    // PRIMARY - Основной цвет бренда
    // ============================================================
    PRIMARY(
        attributeName = "primary",
        description = "Основной цвет бренда (кнопки, FAB, активные элементы)",
        group = ColorGroup.PRIMARY,
    ),
    ON_PRIMARY(
        attributeName = "onPrimary",
        description = "Текст и иконки на основном цвете",
        group = ColorGroup.PRIMARY,
    ),
    PRIMARY_CONTAINER(
        attributeName = "primaryContainer",
        description = "Контейнер основного цвета (фоны важных элементов)",
        group = ColorGroup.PRIMARY,
    ),
    ON_PRIMARY_CONTAINER(
        attributeName = "onPrimaryContainer",
        description = "Текст в контейнере основного цвета",
        group = ColorGroup.PRIMARY,
    ),

    // ============================================================
    // SECONDARY - Акцентный цвет
    // ============================================================
    SECONDARY(
        attributeName = "secondary",
        description = "Акцентный цвет (второстепенные элементы)",
        group = ColorGroup.SECONDARY,
    ),
    ON_SECONDARY(
        attributeName = "onSecondary",
        description = "Текст и иконки на акцентном цвете",
        group = ColorGroup.SECONDARY,
    ),
    SECONDARY_CONTAINER(
        attributeName = "secondaryContainer",
        description = "Контейнер акцентного цвета",
        group = ColorGroup.SECONDARY,
    ),
    ON_SECONDARY_CONTAINER(
        attributeName = "onSecondaryContainer",
        description = "Текст в контейнере акцентного цвета",
        group = ColorGroup.SECONDARY,
    ),

    // ============================================================
    // TERTIARY - Третичный цвет (для карточек)
    // ============================================================
    TERTIARY(
        attributeName = "tertiary",
        description = "Третичный цвет (используется для карточек)",
        group = ColorGroup.TERTIARY,
    ),
    ON_TERTIARY(
        attributeName = "onTertiary",
        description = "Текст на третичном цвете",
        group = ColorGroup.TERTIARY,
    ),
    TERTIARY_CONTAINER(
        attributeName = "tertiaryContainer",
        description = "Фон карточек",
        group = ColorGroup.TERTIARY,
    ),
    ON_TERTIARY_CONTAINER(
        attributeName = "onTertiaryContainer",
        description = "Текст на фоне карточек",
        group = ColorGroup.TERTIARY,
    ),

    // ============================================================
    // BACKGROUND - Фон
    // ============================================================
    BACKGROUND(
        attributeName = "background",
        description = "Фон экранов (под списками, навигацией)",
        group = ColorGroup.BACKGROUND,
    ),
    ON_BACKGROUND(
        attributeName = "onBackground",
        description = "Основной текст на фоне",
        group = ColorGroup.BACKGROUND,
    ),

    // ============================================================
    // SURFACE - Поверхности
    // ============================================================
    SURFACE(
        attributeName = "surface",
        description = "Фон карточек в списках",
        group = ColorGroup.SURFACE,
    ),
    ON_SURFACE(
        attributeName = "onSurface",
        description = "Текст на карточках",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_VARIANT(
        attributeName = "surfaceVariant",
        description = "Вариант поверхности для контраста",
        group = ColorGroup.SURFACE,
    ),
    ON_SURFACE_VARIANT(
        attributeName = "onSurfaceVariant",
        description = "Вторичный текст (описания, подзаголовки)",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_DIM(
        attributeName = "surfaceDim",
        description = "Затемнённая поверхность",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_BRIGHT(
        attributeName = "surfaceBright",
        description = "Освещённая поверхность",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_CONTAINER_LOWEST(
        attributeName = "surfaceContainerLowest",
        description = "Самый низкий уровень поверхности",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_CONTAINER_LOW(
        attributeName = "surfaceContainerLow",
        description = "Низкий уровень поверхности",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_CONTAINER(
        attributeName = "surfaceContainer",
        description = "Средний уровень поверхности",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_CONTAINER_HIGH(
        attributeName = "surfaceContainerHigh",
        description = "Высокий уровень поверхности",
        group = ColorGroup.SURFACE,
    ),
    SURFACE_CONTAINER_HIGHEST(
        attributeName = "surfaceContainerHighest",
        description = "Самый высокий уровень поверхности",
        group = ColorGroup.SURFACE,
    ),

    // ============================================================
    // OUTLINE - Обводка
    // ============================================================
    OUTLINE(
        attributeName = "outline",
        description = "Обводка элементов (TextField, Card)",
        group = ColorGroup.OUTLINE,
    ),
    OUTLINE_VARIANT(
        attributeName = "outlineVariant",
        description = "Вариант обводки для разделителей",
        group = ColorGroup.OUTLINE,
    ),

    // ============================================================
    // ERROR - Ошибки
    // ============================================================
    ERROR(
        attributeName = "error",
        description = "Цвет ошибок и предупреждений",
        group = ColorGroup.ERROR,
    ),
    ON_ERROR(
        attributeName = "onError",
        description = "Текст и иконки на цвете ошибки",
        group = ColorGroup.ERROR,
    ),
    ERROR_CONTAINER(
        attributeName = "errorContainer",
        description = "Фон контейнера ошибки",
        group = ColorGroup.ERROR,
    ),
    ON_ERROR_CONTAINER(
        attributeName = "onErrorContainer",
        description = "Текст в контейнере ошибки",
        group = ColorGroup.ERROR,
    ),

    // ============================================================
    // INVERSE - Инверсные цвета
    // ============================================================
    INVERSE_SURFACE(
        attributeName = "inverseSurface",
        description = "Инверсный цвет поверхности (Snackbar, NavigationDrawer)",
        group = ColorGroup.INVERSE,
    ),
    INVERSE_ON_SURFACE(
        attributeName = "inverseOnSurface",
        description = "Текст на инверсной поверхности",
        group = ColorGroup.INVERSE,
    ),
    INVERSE_PRIMARY(
        attributeName = "inversePrimary",
        description = "Инверсный основной цвет",
        group = ColorGroup.INVERSE,
    ),

    // ============================================================
    // ADDITIONAL - Дополнительные цвета приложения
    // ============================================================
    NAVIGATION_BAR(
        attributeName = "navigationBar",
        description = "Фон нижней панели навигации",
        group = ColorGroup.ADDITIONAL,
    ),
    LINK(
        attributeName = "link",
        description = "Цвет ссылок в тексте",
        group = ColorGroup.ADDITIONAL,
    ),
    CARD_STROKE(
        attributeName = "cardStroke",
        description = "Обводка карточек",
        group = ColorGroup.ADDITIONAL,
    ),
    ;

    /**
     * Группы атрибутов для организации в UI
     */
    enum class ColorGroup {
        PRIMARY,
        SECONDARY,
        TERTIARY,
        BACKGROUND,
        SURFACE,
        OUTLINE,
        ERROR,
        INVERSE,
        ADDITIONAL,
    }

    companion object {
        /**
         * Получить все атрибуты для указанной группы
         */
        fun getByGroup(group: ColorGroup): List<CustomColorAttribute> {
            return entries.filter { it.group == group }
        }

        /**
         * Все атрибуты, сгруппированные по ColorGroup
         */
        fun groupedByCategory(): Map<ColorGroup, List<CustomColorAttribute>> {
            return ColorGroup.entries.associateWith { group ->
                getByGroup(group)
            }
        }
    }
}