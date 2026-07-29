package su.sv.commonui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import su.sv.commonui.R
import su.sv.commonui.ui.shimmerBrush

/**
 * Полноэкранный просмотрщик изображений с поддержкой свайпа и zoom
 *
 * @param images список URL изображений
 * @param initialIndex начальный индекс изображения (по умолчанию 0)
 * @param onDismiss обработчик закрытия
 * @param showNavigationControls показывать ли элементы навигации (кнопка назад, индикатор)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    showNavigationControls: Boolean = true,
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, images.size - 1)) { images.size }
    var isControlsVisible by remember { mutableStateOf(showNavigationControls) }

    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.95f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Pager с изображениями
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ZoomableImage(
                        imageUrl = images[page],
                        onTap = { isControlsVisible = !isControlsVisible },
                        contentDescription = stringResource(
                            R.string.image_viewer_content_description,
                            page + 1,
                            images.size
                        ),
                    )
                }

                // Элементы управления
                if (showNavigationControls) {
                    AnimatedVisibility(
                        visible = isControlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(1f),
                    ) {
                        ViewerTopBar(
                            currentIndex = pagerState.currentPage + 1,
                            totalCount = images.size,
                            onDismiss = onDismiss,
                        )
                    }

                    // Индикатор страниц (для нескольких изображений)
                    if (images.size > 1) {
                        AnimatedVisibility(
                            visible = isControlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f),
                        ) {
                            PageIndicator(
                                currentPage = pagerState.currentPage,
                                totalPages = images.size,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Изображение с поддержкой zoom (через двойной тап)
 */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    onTap: () -> Unit,
    contentDescription: String,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showShimmer by remember { mutableStateOf(true) }
    var imageSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .build(),
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxSize()
            .background(shimmerBrush(targetValue = 1300f, showShimmer = showShimmer))
            .onSizeChanged { imageSize = it }
            .pointerInput(Unit) {
                // Только tap gestures - свайп обрабатывается HorizontalPager
                detectTapGestures(
                    onTap = {
                        // При зуме одиночный тап сбрасывает, иначе показывает/скрывает controls
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            onTap()
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        // Toggle zoom на двойной тап
                        if (scale > 1f) {
                            // Сбрасываем zoom
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // Zoom в точку тапа
                            scale = 2.5f
                            // Центрируем на точке тапа (приблизительно)
                            if (imageSize.width > 0 && imageSize.height > 0) {
                                offsetX = (imageSize.width / 2f - tapOffset.x) * (scale - 1f)
                                offsetY = (imageSize.height / 2f - tapOffset.y) * (scale - 1f)
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            },
        contentScale = ContentScale.Fit,
        onState = { state ->
            if (state is AsyncImagePainter.State.Success) {
                showShimmer = false
            }
        },
    )
}

/**
 * Верхняя панель с кнопкой закрытия и счётчиком
 */
@Composable
private fun ViewerTopBar(
    currentIndex: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.image_viewer_close),
                    tint = Color.White,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "$currentIndex / $totalCount",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp)) // Баланс для кнопки назад
        }
    }
}

/**
 * Индикатор страниц (точки)
 */
@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(totalPages) { index ->
                val isSelected = index == currentPage
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}