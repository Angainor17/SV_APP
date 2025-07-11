package su.sv.books.catalog.domain

import su.sv.books.catalog.data.repo.BookDownloadRepository
import su.sv.books.catalog.data.repo.RemoteBooksRepo
import su.sv.books.catalog.domain.model.Book
import javax.inject.Inject

class GetBooksListUseCase @Inject constructor(
    private val remoteBooksRepo: RemoteBooksRepo,
    private val downloadRepo: BookDownloadRepository,
) {

    suspend fun execute(): Result<List<Book>> {
        return remoteBooksRepo.getBooks().map { list ->
            list.map {
                val id = it.id.orEmpty()
                val fileNameWithExt = it.fileNameWithExt.orEmpty()

                Book(
                    id = id,
                    title = it.title.orEmpty(),
                    description = it.description.orEmpty(),
                    author = it.author.orEmpty(),
                    image = it.image.orEmpty(),
                    link = it.link.orEmpty(),
                    category = it.category.orEmpty(),
                    fileNameWithExt = fileNameWithExt,

                    fileUri = downloadRepo.getDownloadsUri(fileNameWithExt),
                )
            }
        }
    }
}
