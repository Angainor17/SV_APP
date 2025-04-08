package su.sv.reader.presentation.reader

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import su.sv.models.ui.book.UiBook
import su.sv.reader.presentation.reader.mapper.UiBookReaderMapper
import su.sv.reader.presentation.reader.model.BookReaderState
import javax.inject.Inject

@HiltViewModel
class BookReaderViewModel @Inject constructor(
    private val uiMapper: UiBookReaderMapper,
) : BaseViewModel() {

    /** Контент экрана */
    private val _state = MutableStateFlow<BookReaderState>(BookReaderState.Loading)
    val state: StateFlow<BookReaderState> get() = _state

    fun loadBook(book: UiBook) {
        _state.value = uiMapper.createState(book)
    }
}
