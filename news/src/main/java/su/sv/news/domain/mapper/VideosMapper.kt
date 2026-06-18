package su.sv.news.domain.mapper

import su.sv.api.data.response.VkAttachmentPhoto
import su.sv.api.data.response.VkAttachmentVideo
import su.sv.api.data.response.VkPhotoSize
import su.sv.api.data.response.VkResponseNewsAttachment
import su.sv.news.domain.model.NewsMediaItem

/**
 * Целевая ширина изображения для отображения в ленте новостей (в пикселях)
 * При стандартной ширине экрана ~360-400dp, выбираем ~400-500px для хорошего качества
 */
private const val TARGET_IMAGE_WIDTH = 450

/**
 * Минимальная ширина изображения (чтобы не выбирать слишком маленькие)
 */
private const val MIN_IMAGE_WIDTH = 300

/**
 * Приоритет типов размеров фото (от меньшего к большему)
 * Используется как fallback, если размеры не указаны
 */
private val PHOTO_SIZE_PRIORITY = listOf(
    "q", // 320px - минимальный приемлемый
    "r", // 510px - оптимальный
    "x", // 453px - альтернатива
    "y", // 606px - высокое качество
    "z", // 800px - максимальное
)

/**
 * Приоритет типов размеров для видео-превью
 */
private val VIDEO_SIZE_PRIORITY = listOf(
    "q", // 320px
    "r", // 510px
    "x", // 453px
    "y", // 606px
)

fun fromApiToDomain(api: VkResponseNewsAttachment): NewsMediaItem? {
    return when (api.type) {
        "photo" -> fromApiToDomain(api.photo ?: return null)
        "video" -> fromApiToDomain(api.video ?: return null)
        else -> null
    }
}

fun fromApiToDomain(api: VkAttachmentPhoto): NewsMediaItem.ImageItem {
    return NewsMediaItem.ImageItem(
        image = api.getOptimalImageUrl(),
    )
}

fun fromApiToDomain(api: VkAttachmentVideo): NewsMediaItem.VideoItem {
    val id = api.id.toString()
    val ownerId = api.ownerId.orEmpty()

    return NewsMediaItem.VideoItem(
        id = id,
        image = api.getOptimalPreviewUrl(),
        link = "https://vk.com/video${ownerId}_$id",
    )
}

/**
 * Получить URL изображения оптимального размера
 */
fun VkResponseNewsAttachment.getImageUrl(): String? = photo?.getOptimalImageUrl()

/**
 * Получить URL фото оптимального размера из списка sizes
 */
fun VkAttachmentPhoto.getOptimalImageUrl(): String {
    // Если есть sizes - выбираем оптимальный
    sizes?.let { sizeList ->
        return findOptimalSize(sizeList, PHOTO_SIZE_PRIORITY, TARGET_IMAGE_WIDTH)
    }

    // Fallback на origPhoto
    return origPhoto?.url.orEmpty()
}

/**
 * Получить URL превью видео оптимального размера
 */
fun VkAttachmentVideo.getOptimalPreviewUrl(): String {
    image?.let { sizeList ->
        return findOptimalSize(sizeList, VIDEO_SIZE_PRIORITY, TARGET_IMAGE_WIDTH)
    }
    return ""
}

/**
 * Найти оптимальный размер изображения
 *
 * Алгоритм:
 * 1. Фильтруем размеры с валидными URL и шириной >= MIN_IMAGE_WIDTH
 * 2. Сортируем по близости к TARGET_IMAGE_WIDTH
 * 3. Возвращаем ближайший подходящий
 * 4. Если не нашли - используем fallback по типу
 */
private fun findOptimalSize(
    sizes: List<VkPhotoSize>,
    typePriority: List<String>,
    targetWidth: Int,
): String {
    // Фильтруем валидные размеры
    val validSizes = sizes.filter { size ->
        val width = size.width
        !size.url.isNullOrEmpty() && width != null && width >= MIN_IMAGE_WIDTH
    }

    // Если есть размеры с указанной шириной - выбираем ближайший к целевому
    if (validSizes.isNotEmpty()) {
        return validSizes
            .minByOrNull { size ->
                val width = size.width ?: 0
                kotlin.math.abs(width - targetWidth)
            }
            ?.url
            .orEmpty()
    }

    // Fallback: ищем по приоритету типов
    for (type in typePriority) {
        sizes.find { it.type == type && !it.url.isNullOrEmpty() }
            ?.url
            ?.let { return it }
    }

    // Последний fallback: берём последний размер (обычно самый большой)
    return sizes.lastOrNull { !it.url.isNullOrEmpty() }?.url.orEmpty()
}
