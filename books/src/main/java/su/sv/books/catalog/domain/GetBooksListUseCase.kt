package su.sv.books.catalog.domain

import su.sv.books.catalog.data.repo.BooksRepo
import su.sv.books.catalog.domain.model.Book
import javax.inject.Inject

class GetBooksListUseCase @Inject constructor(
    private val booksRepo: BooksRepo,
) {

    suspend fun execute(): Result<List<Book>> {
        return booksRepo.getBooks().map { list ->
            list.map {
                val id = it.id.orEmpty()
                Book(
                    id = id,
                    title = it.title.orEmpty(),
                    image = it.image.orEmpty(),
                    link = it.link.orEmpty(),

                    fileUri = booksRepo.getDownloadedBookUri(id),
                )
            }
        }
    }
}
