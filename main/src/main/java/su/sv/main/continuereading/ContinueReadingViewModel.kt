package su.sv.main.continuereading

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.axet.bookreader.domain.GetLastReadBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
            Log.d(TAG, "loadAndCheck: already interacted, skipping")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "loadAndCheck: loading last read book")

            val bookInfo = getLastReadBookUseCase()

            if (bookInfo == null) {
                Log.d(TAG, "loadAndCheck: no last read book found")
                _state.value = ContinueReadingState.Hidden
                return@launch
            }

            Log.d(TAG, "loadAndCheck: found book: ${bookInfo.title}")
            _state.value = ContinueReadingState.Visible(bookInfo)
        }
    }

    /**
     * Продолжить чтение - открыть книгу.
     */
    fun onContinueClick() {
        val currentState = _state.value as? ContinueReadingState.Visible ?: return
        val bookInfo = currentState.bookInfo

        Log.d(TAG, "onContinueClick: opening book: ${bookInfo.title}")

        viewModelScope.launch {
            _effect.emit(
                ContinueReadingEffect.OpenBook(
                    bookUri = Uri.parse(bookInfo.bookFileUri),
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
        Log.d(TAG, "onDismissClick: hiding snackbar")
        hideSnackbar()
    }

    private fun hideSnackbar() {
        wasInteracted = true
        _state.value = ContinueReadingState.Hidden
    }
}