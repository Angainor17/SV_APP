package su.sv.reader.presentation.reader.mapper

import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.VerticalPdfReaderState
import su.sv.models.ui.book.UiBook
import su.sv.reader.presentation.reader.model.BookReaderState
import javax.inject.Inject

class UiBookReaderMapper @Inject constructor(

) {
    fun createState(book: UiBook): BookReaderState.Content {
        return BookReaderState.Content(
            pdfReaderState = createPdfReaderState(book)
        )
    }

    private fun createPdfReaderState(book: UiBook): VerticalPdfReaderState {
        return VerticalPdfReaderState(
            resource = ResourceType.Local(book.fileUri ?: error("Check screen open logic")),
            isZoomEnable = true
        )
    }
}
