package su.sv.managers

import timber.log.Timber
import javax.inject.Inject

class OnBookPageListener @Inject constructor() {

    fun tellAboutMisspell(
        text: String,
        book: String,
    ) {
        Timber.tag("voronin").d("tellAboutMisspell = $book")
    }

    fun askQuestion(
        text: String,
        book: String,
    ) {
        Timber.tag("voronin").d("askQuestion = $book")
    }
}
