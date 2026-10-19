package su.sv.commonui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Размеры и отступы SV APP
 * Единая система измерений для всего приложения
 */
data class AppDimensions(
    // ============================================================
    // SCREEN PADDING - Отступы от краёв экрана
    // ============================================================

    /** Стандартный горизонтальный отступ от краёв экрана */
    val screenPaddingHorizontal: Dp = 16.dp,

    /** Увеличенный горизонтальный отступ (для планшетов) */
    val screenPaddingHorizontalLarge: Dp = 24.dp,

    /** Стандартный вертикальный отступ */
    val screenPaddingVertical: Dp = 16.dp,

    // ============================================================
    // CARD PADDING - Внутренние отступы карточек
    // ============================================================

    /** Внутренний отступ карточки */
    val cardPaddingInner: Dp = 12.dp,

    /** Внешний отступ карточки (от других элементов) */
    val cardPaddingOuter: Dp = 8.dp,

    /** Горизонтальный отступ контента внутри карточки */
    val cardContentPaddingHorizontal: Dp = 12.dp,

    /** Вертикальный отступ контента внутри карточки */
    val cardContentPaddingVertical: Dp = 8.dp,

    // ============================================================
    // ITEM SPACING - Отступы между элементами
    // ============================================================

    /** Малый отступ между элементами */
    val itemSpacingSmall: Dp = 4.dp,

    /** Средний отступ между элементами */
    val itemSpacingMedium: Dp = 8.dp,

    /** Большой отступ между элементами */
    val itemSpacingLarge: Dp = 16.dp,

    /** Очень большой отступ между элементами */
    val itemSpacingXLarge: Dp = 24.dp,

    /** Огромный отступ между элементами */
    val itemSpacingXXLarge: Dp = 32.dp,

    // ============================================================
    // LIST PADDING - Отступы списков
    // ============================================================

    /** Вертикальный отступ элемента списка */
    val listItemPaddingVertical: Dp = 8.dp,

    /** Горизонтальный отступ элемента списка */
    val listItemPaddingHorizontal: Dp = 16.dp,

    /** Отступ между элементами в списке */
    val listItemSpacing: Dp = 8.dp,

    // ============================================================
    // COMPONENT HEIGHTS - Высоты компонентов
    // ============================================================

    /** Высота тулбара */
    val toolbarHeight: Dp = 56.dp,

    /** Высота нижней навигации */
    val bottomNavHeight: Dp = 80.dp,

    /** Высота стандартной кнопки */
    val buttonHeight: Dp = 48.dp,

    /** Высота малой кнопки */
    val buttonHeightSmall: Dp = 36.dp,

    /** Высота большой кнопки */
    val buttonHeightLarge: Dp = 56.dp,

    /** Высота элемента списка */
    val listItemHeight: Dp = 72.dp,

    /** Минимальная высота карточки */
    val cardMinHeight: Dp = 120.dp,

    // ============================================================
    // ICON SIZES - Размеры иконок
    // ============================================================

    /** Стандартный размер иконки */
    val iconSizeStandard: Dp = 24.dp,

    /** Малый размер иконки */
    val iconSizeSmall: Dp = 18.dp,

    /** Большой размер иконки */
    val iconSizeLarge: Dp = 32.dp,

    /** Очень большой размер иконки */
    val iconSizeXLarge: Dp = 48.dp,

    /** Размер иконки в тулбаре */
    val iconSizeToolbar: Dp = 24.dp,

    /** Размер иконки в FAB */
    val iconSizeFab: Dp = 24.dp,

    // ============================================================
    // TOUCH TARGET - Область касания (Accessibility)
    // ============================================================

    /** Минимальная область касания (Accessibility guideline) */
    val minTouchTarget: Dp = 48.dp,

    // ============================================================
    // LOADING INDICATOR - Индикатор загрузки
    // ============================================================

    /** Размер индикатора загрузки */
    val loadingIndicatorSize: Dp = 32.dp,

    /** Малый размер индикатора загрузки */
    val loadingIndicatorSizeSmall: Dp = 20.dp,

    /** Большой размер индикатора загрузки */
    val loadingIndicatorSizeLarge: Dp = 48.dp,

    // ============================================================
    // ELEVATION - Возвышение (тени)
    // ============================================================

    /** Стандартное возвышение карточки */
    val cardElevation: Dp = 2.dp,

    /** Возвышение при нажатии */
    val cardElevationPressed: Dp = 4.dp,

    /** Возвышение диалога */
    val dialogElevation: Dp = 6.dp,

    // ============================================================
    // BORDER WIDTH - Толщина границ
    // ============================================================

    /** Толщина стандартной границы */
    val borderWidthStandard: Dp = 1.dp,

    /** Толщина жирной границы */
    val borderWidthThick: Dp = 2.dp,

    // ============================================================
    // AVATAR - Аватары
    // ============================================================

    /** Размер малого аватара */
    val avatarSizeSmall: Dp = 32.dp,

    /** Размер стандартного аватара */
    val avatarSizeStandard: Dp = 40.dp,

    /** Размер большого аватара */
    val avatarSizeLarge: Dp = 56.dp,

    // ============================================================
    // CORNER RADIUS - Радиусы скругления (дублируют Shape для удобства)
    // ============================================================

    /** Малый радиус скругления */
    val cornerRadiusSmall: Dp = 8.dp,

    /** Средний радиус скругления */
    val cornerRadiusMedium: Dp = 12.dp,

    /** Большой радиус скругления */
    val cornerRadiusLarge: Dp = 16.dp,
) {
    companion object {
        /** Экземпляр по умолчанию */
        val Default = AppDimensions()
    }
}

/**
 * CompositionLocal для доступа к размерам из любого места в Compose
 */
val LocalAppDimensions = staticCompositionLocalOf { AppDimensions.Default }
