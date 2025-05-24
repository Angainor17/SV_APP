package su.sv.managers

import timber.log.Timber
import javax.inject.Inject

class OnBookPagerManager @Inject constructor() {

    fun tellAboutMisspell(
        text: String,
        title: String,
        author: String,
        page: Int,
    ) {
        Timber.tag("voronin").d("text = $text")
        Timber.tag("voronin").d("title = $title")
        Timber.tag("voronin").d("author = $author")
        Timber.tag("voronin").d("page = $page")
    }

    fun askQuestion(
        text: String,
        title: String,
        author: String,
        page: Int,
    ) {
        Timber.tag("voronin").d("text = $text")
        Timber.tag("voronin").d("title = $title")
        Timber.tag("voronin").d("author = $author")
        Timber.tag("voronin").d("page = $page")
    }
}
