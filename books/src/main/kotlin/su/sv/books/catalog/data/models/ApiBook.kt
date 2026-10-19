package su.sv.books.catalog.data.models

import com.google.gson.annotations.SerializedName

class ApiBook(

    /** Любой идентификатор, в целом - не обязательно */
    @SerializedName("ixBook") val id: String?,

    /** Название */
    @SerializedName("sTitle") val title: String?,

    /** Описание, которые отобразим при нажатии на элемент списка */
    @SerializedName("sDescription") val description: String?,

    /** Автор */
    @SerializedName("sAuthor") val author: String?,

    /** Категория */
    @SerializedName("sCategory") val category: String?,

    /** Картинка обложки для списка */
    @SerializedName("sCoverLink") val image: String?,

    /** Ссылка для скачивания (не на яндекс диск, а именно что по переходу начнётся скачивание) */
    @SerializedName("sDownloadLink") val link: String?,

    /** Тут название файла, который скачается, в целом не обязательно */
    @SerializedName("sFilename") val fileNameWithExt: String?,
)