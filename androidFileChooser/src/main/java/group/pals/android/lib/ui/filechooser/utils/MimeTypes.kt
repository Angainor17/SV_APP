/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

/**
 * MIME-типы для файлов.
 *
 * @author Hai Bison
 * @since v4.5 beta
 */
object MimeTypes {

    val regexFileTypePlainTexts = "(?si).+\\.(txt|html?|json|csv|java|pas|php.*|c|cpp|" +
            "bas|python|js|javascript|scala|xml|kml|css|ps|xslt?|tpl|tsv|bash|cmd|pl|pm|ps1|ps1xml|psc1|psd1|psm1|" +
            "py|pyc|pyo|r|rb|sdl|sh|tcl|vbs|xpl|ada|adb|ads|clj|cls|cob|cbl|cxx|cs|csproj|d|e|el|go|h|hpp|hxx|l|" +
            "m|url|ini|prop|conf|properties|rc)".toRegex()

    val regexFileTypeHtmls = "(?si).+\\.(html?)".toRegex()

    /**
     * @see [Форматы файлов изображений](http://en.wikipedia.org/wiki/Image_file_formats)
     */
    val regexFileTypeImages = "(?si).+\\.(gif|jpe?g|png|tiff?|wmf|emf|jfif|exif|" +
            "raw|bmp|ppm|pgm|pbm|pnm|webp|riff|tga|ilbm|img|pcx|ecw|sid|cd5|fits|pgf|xcf|svg|pns|jps|icon?|" +
            "jp2|mng|xpm|djvu)".toRegex()

    /**
     * @see [Форматы аудиофайлов](http://en.wikipedia.org/wiki/Audio_file_format)
     * @see [Список форматов файлов](http://en.wikipedia.org/wiki/List_of_file_formats)
     */
    val regexFileTypeAudios = "(?si).+\\.(mp[2-3]+|wav|aiff|au|m4a|ogg|raw|flac|" +
            "mid|amr|aac|alac|atrac|awb|m4p|mmf|mpc|ra|rm|tta|vox|wma)".toRegex()

    /**
     * @see [Форматы видеофайлов](http://en.wikipedia.org/wiki/Video_file_formats)
     */
    val regexFileTypeVideos = "(?si).+\\.(mp[4]+|flv|wmv|webm|m4v|3gp|mkv|mov|mpe?g|rmv?|ogv|" +
            "avi)".toRegex()

    /**
     * @see [Список форматов файлов](http://en.wikipedia.org/wiki/List_of_file_formats)
     */
    val regexFileTypeCompressed = "(?si).+\\.(zip|7z|lz?|[jrt]ar|gz|gzip|bzip|xz|cab|sfx|" +
            "z|iso|bz?|rz|s7z|apk|dmg)".toRegex()
}
