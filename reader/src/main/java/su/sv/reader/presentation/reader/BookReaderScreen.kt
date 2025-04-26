package su.sv.reader.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.models.ui.book.UiBook
import su.sv.reader.databinding.ViewPdfReaderBinding

@Parcelize
class BookReaderScreen(
    private val uiBook: UiBook,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
//        BookReader(uiBook, modifier)
        AndroidViewBinding(modifier = modifier, factory = { inflater, parent, attachToParent ->
            ViewPdfReaderBinding.inflate(
                inflater,
                parent,
                false
            )
        }) {
            init(pdfView, uiBook)
        }
    }
}

private fun init(pdfView: PDFView, uiBook: UiBook) {
    pdfView.fromUri(uiBook.fileUri!!)
//        .pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
        .enableSwipe(true) // allows to block changing pages using swipe
        .swipeHorizontal(false)
        .enableDoubletap(true)
//        .defaultPage(0)
// allows to draw something on the current page, usually visible in the middle of the screen
//        .onDraw(onDrawListener)
//// allows to draw something on all pages, separately for every page. Called only for visible pages
//        .onDrawAll(onDrawListener)
//        .onLoad(onLoadCompleteListener) // called after document is loaded and starts to be rendered
//        .onPageChange(onPageChangeListener)
//        .onPageScroll(onPageScrollListener)
//        .onError(onErrorListener)
//        .onPageError(onPageErrorListener)
//        .onRender(onRenderListener) // called after document is rendered for the first time
//// called on single tap, return true if handled, false to toggle scroll handle visibility
//        .onTap(onTapListener)
//        .onLongPress(onLongPressListener)
        .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
//        .password(null)
//        .scrollHandle(null)
//        .enableAntialiasing(true) // improve rendering a little bit on low-res screens
// spacing between pages in dp. To define spacing color, set view background
        .spacing(0)
        .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen
//        .linkHandler(DefaultLinkHandler(pdfView)) // FIXME
        .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
        .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
        .pageSnap(false) // snap pages to screen boundaries
        .pageFling(false) // make a fling change only a single page like ViewPager
        .nightMode(false) // toggle night mode
        .load()
}
