package su.sv.books.catalog.data.api

import kotlinx.coroutines.delay
import su.sv.books.catalog.data.models.ApiBook
import java.time.LocalDate
import javax.inject.Inject

class BooksApiMock @Inject constructor() : BooksApi {

    override suspend fun getBooks(): List<ApiBook> {
        delay(3_000)

        return listOf(
            ApiBook(
                id = "0",
                title = "ОВЛ том №12",
                description = "",
                image = "https://svg-shop2.ru/userfls/shop/large/138_ovl-tom-12--uroki-pervoy-ru.png",
                pagesCount = 746,
                publicationDate = LocalDate.now().minusYears(1),
                link = "",
                fileNameWithExt = "asdasdasd.pdf",
            ),
            ApiBook(
                id = "1",
                title = "Научный социализм ".repeat(1),
                description = "",
                image = "https://bibl.fra-mos.ru/wp-content/uploads/2024/03/cover.png",
                pagesCount = 746,
                link = "https://disk.yandex.ru/i/q_14zxDRofkxUw",
                fileNameWithExt = "1.pdf",
                publicationDate = LocalDate.now().minusYears(1).minusMonths(3),
            ),
            ApiBook(
                id = "2",
                title = "НАУКА ПОБЕЖДАТЬ",
                description = "учебное пособие КомКружка ФРА",
                image = "https://svg-shop2.ru/userfls/shop/large/92_nauka-pobezhdat-uchebnoe-po.png",
                pagesCount = 258,
                link = "https://disk.yandex.ru/i/eTSpeIfg0JheWw",
                publicationDate = LocalDate.now().minusYears(2).minusMonths(1),
                fileNameWithExt = "2.pdf",
            ),
            ApiBook(
                id = "3",
                title = "НАУКА ЛОГИКИ",
                description = "Г.В.Ф. Гегель",
                image = "https://bibl.fra-mos.ru/wp-content/uploads/2016/12/Hegel_SL_Cover.jpg",
                pagesCount = 544,
                link = "https://bibl.fra-mos.ru/wp-content/uploads/2020/04/Hegel_T1.pdf",
                publicationDate = LocalDate.now().minusDays(22).minusMonths(1),
                fileNameWithExt = "Hegel_T1.pdf",
            ),
        )
    }
}
