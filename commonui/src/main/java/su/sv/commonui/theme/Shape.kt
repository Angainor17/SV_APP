package su.sv.commonui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Формы (радиусы скругления) SV APP
 * Основано на Material Design 3
 */
data class AppShapes(
    // ============================================================
    // CORNER RADIUS - Радиусы скругления
    // ============================================================

    /** Очень малый радиус (chips, tags) */
    val cornerExtraSmall: RoundedCornerShape = RoundedCornerShape(4.dp),

    /** Малый радиус (кнопки, текстовые поля) */
    val cornerSmall: RoundedCornerShape = RoundedCornerShape(8.dp),

    /** Средний радиус (карточки) */
    val cornerMedium: RoundedCornerShape = RoundedCornerShape(12.dp),

    /** Большой радиус (большие карточки, диалоги) */
    val cornerLarge: RoundedCornerShape = RoundedCornerShape(16.dp),

    /** Очень большой радиус (bottom sheets) */
    val cornerExtraLarge: RoundedCornerShape = RoundedCornerShape(28.dp),

    /** Полностью круглый (FAB, аватары) */
    val cornerFull: RoundedCornerShape = RoundedCornerShape(50),

    // ============================================================
    // SPECIFIC SHAPES - Специфичные формы
    // ============================================================

    /** Форма карточки */
    val cardShape: Shape = cornerMedium,

    /** Форма кнопки */
    val buttonShape: Shape = cornerSmall,

    /** Форма диалога */
    val dialogShape: Shape = cornerExtraLarge,

    /** Форма bottom sheet */
    val bottomSheetShape: Shape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    ),

    /** Форма текстового поля */
    val textFieldShape: Shape = cornerSmall,

    /** Форма chip */
    val chipShape: Shape = cornerExtraSmall,

    /** Форма FAB */
    val fabShape: Shape = cornerLarge,

    /** Форма аватара */
    val avatarShape: Shape = cornerFull,

    // ============================================================
    // SWIPE ACTION SHAPES - Формы для swipe-действий
    // ============================================================

    /** Форма иконки при свайпе */
    val swipeActionShape: Shape = cornerSmall,
) {
    companion object {
        /** Экземпляр по умолчанию */
        val Default = AppShapes()
    }
}

/**
 * Преобразование AppShapes в Material3 Shapes
 */
fun AppShapes.toMaterialShapes(): Shapes = Shapes(
    extraSmall = cornerExtraSmall,
    small = cornerSmall,
    medium = cornerMedium,
    large = cornerLarge,
    extraLarge = cornerExtraLarge
)

/**
 * CompositionLocal для доступа к формам из любого места в Compose
 */
val LocalAppShapes = staticCompositionLocalOf { AppShapes.Default }
