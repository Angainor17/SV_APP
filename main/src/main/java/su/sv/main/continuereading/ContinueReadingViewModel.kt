package su.sv.main.continuereading

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.axet.bookreader.domain.GetLastReadBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import su.sv.main.R
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "ContinueReading"

/**
 * Эффекты от snackbar "Продолжить чтение".
 */
sealed class ContinueReadingEffect {
    /**
     * Открыть книгу для чтения.
     *
     * @param bookUri URI файла книги
     * @param bookTitle название книги
     * @param bookAuthor автор книги
     * @param coverUrl URL обложки
     */
    data class OpenBook(
        val bookUri: Uri,
        val bookTitle: String?,
        val bookAuthor: String?,
        val coverUrl: String?,
    ) : ContinueReadingEffect()
}

/**
 * ViewModel для управления состоянием snackbar "Продолжить чтение".
 */
@HiltViewModel
class ContinueReadingViewModel @Inject constructor(
    private val getLastReadBookUseCase: GetLastReadBookUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<ContinueReadingState>(ContinueReadingState.Hidden)
    val state: StateFlow<ContinueReadingState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ContinueReadingEffect>()
    val effect: SharedFlow<ContinueReadingEffect> = _effect.asSharedFlow()

    /**
     * Флаг для отслеживания, что пользователь уже взаимодействовал с snackbar.
     * После клика "Продолжить" или закрытия - не показывать снова в этой сессии.
     */
    private var wasInteracted = false

    /**
     * Загрузить данные и проверить необходимость показа snackbar.
     * Вызывается после загрузки главного экрана.
     */
    fun loadAndCheck() {
        // Не показывать snackbar если пользователь уже взаимодействовал с ним
        if (wasInteracted) {
            Timber.tag(TAG).d("loadAndCheck: already interacted, skipping")
            return
        }

        Timber.tag(TAG).d("loadAndCheck: loading last read book")

        viewModelScope.launch {
            Timber.tag(TAG).d("loadAndCheck: fetching last read book, wasInteracted=$wasInteracted")

            val bookInfo = getLastReadBookUseCase(
                defaultTitle = context.getString(R.string.default_book_title)
            )

            if (bookInfo == null) {
                Timber.tag(TAG).d("loadAndCheck: no last read book found, setting Hidden")
                _state.value = ContinueReadingState.Hidden
                return@launch
            }

            Timber.tag(TAG).d(
                "loadAndCheck: found book='${bookInfo.title}': uri=${bookInfo.bookFileUri}"
            )
            _state.value = ContinueReadingState.Visible(bookInfo)
        }
    }

    /**
     * Продолжить чтение - открыть книгу.
     */
    fun onContinueClick() {
        val currentState = _state.value as? ContinueReadingState.Visible ?: return
        val bookInfo = currentState.bookInfo

        Timber.tag(TAG).d(
            "onContinueClick: opening book='${bookInfo.title}', uri=${bookInfo.bookFileUri}"
        )

        viewModelScope.launch {
            _effect.emit(
                ContinueReadingEffect.OpenBook(
                    bookUri = bookInfo.bookFileUri.toUri(),
                    bookTitle = bookInfo.title,
                    bookAuthor = bookInfo.authors,
                    coverUrl = bookInfo.coverUrl,
                )
            )
        }

        hideSnackbar()
    }

    /**
     * Закрыть snackbar.
     */
    fun onDismissClick() {
        Timber.tag(TAG).d("onDismissClick: hiding snackbar")
        hideSnackbar()
    }

    private fun hideSnackbar() {
        wasInteracted = true
        _state.value = ContinueReadingState.Hidden
    }
}