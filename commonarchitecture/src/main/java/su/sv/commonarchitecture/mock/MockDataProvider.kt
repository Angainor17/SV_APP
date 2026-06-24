package su.sv.commonarchitecture.mock

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Провайдер мок-данных для сетевых запросов.
 *
 * Содержит мок-ответы для всех API:
 * - Wiki API (svremya.su)
 * - Books API (svremya.org)
 * - VK API (api.vk.com)
 *
 * @since 2026-06-24
 */
@Singleton
class MockDataProvider @Inject constructor(
    private val gson: Gson
) {

    // =====================================
    // Wiki API Mocks (svremya.su)
    // =====================================

    /**
     * Мок для поиска статей Wiki.
     * Используется для action=query&list=search
     */
    private val wikiSearchMock: String
        get() = """
        {
            "query": {
                "searchinfo": {
                    "totalhits": 3
                },
                "search": [
                    {
                        "ns": 0,
                        "title": "Ленин, Владимир Ильич",
                        "pageid": 1,
                        "size": 125000,
                        "wordcount": 15000,
                        "snippet": "Владимир Ильич Ленин — российский революционер, советский политический и государственный деятель...",
                        "timestamp": "2024-01-15T10:30:00Z"
                    },
                    {
                        "ns": 0,
                        "title": "Маркс, Карл",
                        "pageid": 2,
                        "size": 98000,
                        "wordcount": 12000,
                        "snippet": "Карл Маркс — немецкий философ, экономист, социолог, политический журналист...",
                        "timestamp": "2024-01-14T08:20:00Z"
                    },
                    {
                        "ns": 0,
                        "title": "Социализм",
                        "pageid": 3,
                        "size": 75000,
                        "wordcount": 9000,
                        "snippet": "Социализм — политическое, социальное и экономическое движение, идеология...",
                        "timestamp": "2024-01-13T12:45:00Z"
                    }
                ]
            },
            "batchcomplete": ""
        }
        """.trimIndent()

    /**
     * Мок для получения страницы Wiki.
     * Используется для action=parse&page=TITLE
     */
    private val wikiPageMock: String
        get() = """
        {
            "parse": {
                "title": "Ленин, Владимир Ильич",
                "pageid": 1,
                "text": {
                    "*": "<div class=\"mw-parser-output\"><p><b>Владимир Ильич Ленин</b> (1870—1924) — российский революционер, советский политический и государственный деятель, создатель партии большевиков, один из организаторов и руководителей Октябрьской революции 1917 года в России.</p><h2>Биография</h2><p>Родился 10 (22) апреля 1870 года в Симбирске. Окончил Симбирскую гимназию. В 1887 году поступил на юридический факультет Казанского университета.</p><h2>Политическая деятельность</h2><p>В 1895 году участвовал в создании петербургского «Союза борьбы за освобождение рабочего класса». В 1903 году возглавил фракцию большевиков в РСДРП.</p></div>"
                },
                "links": [
                    {"ns": 0, "exists": "", "*": "Маркс, Карл"},
                    {"ns": 0, "exists": "", "*": "Революция"},
                    {"ns": 0, "exists": "", "*": "Коммунизм"},
                    {"ns": 0, "exists": "", "*": "СССР"}
                ],
                "displaytitle": "Ленин, Владимир Ильич"
            }
        }
        """.trimIndent()

    /**
     * Мок для OpenSearch Wiki.
     * Возвращает массив: [query, titles[], descriptions[], urls[]]
     */
    private val wikiOpenSearchMock: String
        get() = """
        [
            "Лен",
            ["Ленин, Владимир Ильич", "Ленинград", "Ленинский район"],
            ["Владимир Ильич Ленин — российский революционер", "Ленинград — название Санкт-Петербурга", "Ленинский район — название районов"],
            ["https://svremya.su/index.php/Ленин,_Владимир_Ильич", "https://svremya.su/index.php/Ленинград", "https://svremya.su/index.php/Ленинский_район"]
        ]
        """.trimIndent()

    // =====================================
    // Books API Mocks (svremya.org)
    // =====================================

    /**
     * Мок для списка книг.
     * Переиспользованы данные из BooksApiMock.
     */
    private val booksMock: String
        get() = """
        [
            {
                "ixBook": "0",
                "sTitle": "ОВЛ том №12",
                "sDescription": "В двенадцатый том серии «Основное в ленинизме» вошли произведения, написанные в самый разгар первой буржуазно-демократической революции в России (октябрь 1905 — апрель 1906).",
                "sAuthor": "В. И. Ленин",
                "sCategory": "Свободное время",
                "sCoverLink": "https://svg-shop2.ru/userfls/shop/large/138_ovl-tom-12--uroki-pervoy-ru.png",
                "sDownloadLink": "https://www.rulit.me/download-books-177130.html?t=epub",
                "sFilename": "asdasdasd.pdf"
            },
            {
                "ixBook": "1",
                "sTitle": "Научный социализм",
                "sDescription": "Научный социализм – это марксистско-ленинская теория развития человеческого общества. Наше учебное пособие на 90 % скроено из произведений классиков марксизма-ленинизма.",
                "sAuthor": "Попов М. В., Удовиченко М. С.",
                "sCategory": "Свободное время",
                "sCoverLink": "https://bibl.fra-mos.ru/wp-content/uploads/2024/03/cover.png",
                "sDownloadLink": "https://example.com/book1.fb2",
                "sFilename": "socializm.fb2"
            },
            {
                "ixBook": "2",
                "sTitle": "НАУКА ПОБЕЖДАТЬ",
                "sDescription": "Книга, которую вы держите в руках, - первый шаг в направлении к обществу будущего. В форме бесед авторы излагают основные идеи коммунизма.",
                "sAuthor": "Попов М. В., Удовиченко М. С.",
                "sCategory": "Свободное время",
                "sCoverLink": "https://svg-shop2.ru/userfls/shop/large/92_nauka-pobezhdat-uchebnoe-po.png",
                "sDownloadLink": "https://example.com/book2.pdf",
                "sFilename": "2.pdf"
            }
        ]
        """.trimIndent()

    // =====================================
    // VK API Mocks (api.vk.com)
    // =====================================

    /**
     * Мок для получения постов со стены VK.
     * Используется для /method/wall.get
     */
    private val vkPostsMock: String
        get() = """
        {
            "response": {
                "count": 2,
                "items": [
                    {
                        "id": 12345,
                        "date": ${System.currentTimeMillis() / 1000},
                        "text": "Важная статья о развитии социалистической мысли в XXI веке. Рекомендуем к прочтению!",
                        "attachments": [
                            {
                                "type": "photo",
                                "photo": {
                                    "id": 456789012,
                                    "sizes": [
                                        {"type": "s", "url": "https://sun1.example.com/s.jpg", "width": 75, "height": 56},
                                        {"type": "m", "url": "https://sun1.example.com/m.jpg", "width": 130, "height": 97},
                                        {"type": "x", "url": "https://sun1.example.com/x.jpg", "width": 604, "height": 453}
                                    ]
                                }
                            }
                        ]
                    },
                    {
                        "id": 12346,
                        "date": ${System.currentTimeMillis() / 1000 - 86400},
                        "text": "Новая книга из серии «Основное в ленинизме» уже доступна для скачивания. Том 12 охватывает период первой русской революции.",
                        "attachments": [
                            {
                                "type": "photo",
                                "photo": {
                                    "id": 456789013,
                                    "sizes": [
                                        {"type": "s", "url": "https://sun2.example.com/s.jpg", "width": 75, "height": 56},
                                        {"type": "m", "url": "https://sun2.example.com/m.jpg", "width": 130, "height": 97},
                                        {"type": "x", "url": "https://sun2.example.com/x.jpg", "width": 604, "height": 453}
                                    ]
                                }
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()

    /**
     * Мок для получения видео VK.
     * Используется для /method/video.get
     */
    private val vkVideoMock: String
        get() = """
        {
            "response": {
                "count": 1,
                "items": [
                    {
                        "id": 456789,
                        "date": ${System.currentTimeMillis() / 1000},
                        "text": "Документальный фильм о Владимире Ильиче Ленине",
                        "attachments": [
                            {
                                "type": "video",
                                "video": {
                                    "id": 456789,
                                    "owner_id": "-12345678",
                                    "text": "Фильм рассказывает о жизни и деятельности В.И. Ленина",
                                    "description": "Документальный фильм",
                                    "access_key": "abc123",
                                    "image": [
                                        {"type": "s", "url": "https://sun1.example.com/video_s.jpg", "width": 130, "height": 97},
                                        {"type": "m", "url": "https://sun1.example.com/video_m.jpg", "width": 320, "height": 240}
                                    ]
                                }
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()

    // =====================================
    // Public Methods
    // =====================================

    /**
     * Возвращает мок-ответ для Wiki API.
     *
     * @param request HTTP запрос
     * @return Response с мок-данными
     */
    fun getWikiMock(request: Request): Response {
        val url = request.url.toString()

        val mockBody = when {
            url.contains("action=parse") -> {
                Timber.d("MockDataProvider: Returning Wiki page mock")
                wikiPageMock
            }
            url.contains("action=opensearch") -> {
                Timber.d("MockDataProvider: Returning Wiki opensearch mock")
                wikiOpenSearchMock
            }
            else -> {
                Timber.d("MockDataProvider: Returning Wiki search mock")
                wikiSearchMock
            }
        }

        return createMockResponse(request, mockBody)
    }

    /**
     * Возвращает мок-ответ для Books API.
     *
     * @param request HTTP запрос
     * @return Response с мок-данными
     */
    fun getBooksMock(request: Request): Response {
        Timber.d("MockDataProvider: Returning Books mock")
        return createMockResponse(request, booksMock)
    }

    /**
     * Возвращает мок-ответ для VK API.
     *
     * @param request HTTP запрос
     * @return Response с мок-данными
     */
    fun getVkMock(request: Request): Response {
        val url = request.url.toString()

        val mockBody = when {
            url.contains("video.get") -> {
                Timber.d("MockDataProvider: Returning VK video mock")
                vkVideoMock
            }
            else -> {
                Timber.d("MockDataProvider: Returning VK posts mock")
                vkPostsMock
            }
        }

        return createMockResponse(request, mockBody)
    }

    // =====================================
    // Private Helpers
    // =====================================

    /**
     * Создает mock Response с заданным телом.
     */
    private fun createMockResponse(request: Request, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody(JSON_MEDIA_TYPE))
            .build()
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
