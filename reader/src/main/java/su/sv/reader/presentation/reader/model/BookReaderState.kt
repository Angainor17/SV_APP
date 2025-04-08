package su.sv.reader.presentation.reader.model

import com.rizzi.bouquet.VerticalPdfReaderState

sealed class BookReaderState {

    data class Content(
        val pdfReaderState: VerticalPdfReaderState,
    ) : BookReaderState()

    data object Loading : BookReaderState()
}
