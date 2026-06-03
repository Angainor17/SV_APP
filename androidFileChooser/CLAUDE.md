# AndroidFileChooser Module

Компонент для выбора файлов.

## Обзор

Модуль `androidFileChooser` предоставляет Activity для выбора файлов в файловой системе устройства.

**Язык:** Kotlin

## Основные компоненты

### FileChooserActivity
Activity для выбора файлов:

```kotlin
val intent = Intent(context, FileChooserActivity::class.java)
intent.putExtra(FileChooserActivity._Rootpath, rootDir)
intent.putExtra(FileChooserActivity._FilterMode, FilterMode.FilesOnly)
startActivityForResult(intent, REQUEST_CODE)
```

### IFile
Интерфейс для абстракции над файлами:

```kotlin
interface IFile : Parcelable {
    fun getAbsolutePath(): String
    fun getName(): String
    fun getSecondName(): String
    fun isDirectory(): Boolean
    fun isFile(): Boolean
    fun length(): Long
    fun lastModified(): Long
    fun parentFile(): IFile?
    fun exists(): Boolean
    fun mkdir(): Boolean
    fun delete(): Boolean
    fun equalsToPath(file: IFile?): Boolean
    fun clone(): IFile
    fun canRead(): Boolean
}
```

### IFileAdapter
Адаптер для отображения списка файлов.

### IFileFilter
Фильтр для файлов (функциональный интерфейс):

```kotlin
fun interface IFileFilter {
    fun accept(pathname: IFile): Boolean
}
```

### IFileDataModel
Модель данных для файлов.

## Настройки (Prefs)

### DisplayPrefs
Настройки отображения:

```kotlin
DisplayPrefs.setShowHiddenFiles(context, true)
DisplayPrefs.setSortType(context, SortType.SortByName)
DisplayPrefs.setViewType(context, ViewType.List)
```

### Prefs
Общие настройки файлового менеджера.

## Утилиты

### FileUtils
Утилиты для работы с файлами:

```kotlin
FileUtils.getResIcon(file, filterMode)
FileUtils.isFilenameValid(name)
FileUtils.createDeleteFileThread(file, provider, recursive)
```

### Utils
Общие утилиты (проверка разрешений).

### DateUtils
Утилиты для форматирования дат.

### Converter
Преобразование размера файлов в читаемый формат.

## Структура файлов

```
androidFileChooser/src/main/java/group/pals/android/lib/ui/filechooser/
├── FileChooserActivity.kt
├── IFileAdapter.kt
├── IFileDataModel.kt
├── io/
│   ├── IFile.kt
│   ├── IFileFilter.kt
│   └── localfile/
│       ├── LocalFile.kt
│       └── ParentFile.kt
├── prefs/
│   ├── DisplayPrefs.kt
│   └── Prefs.kt
├── services/
│   ├── IFileProvider.kt
│   ├── FileProviderService.kt
│   └── LocalFileProvider.kt
└── utils/
    ├── FileUtils.kt
    ├── Utils.kt
    ├── DateUtils.kt
    ├── Converter.kt
    ├── TextUtils.kt
    ├── ActivityCompat.kt
    ├── Ui.kt
    ├── E.kt
    ├── MimeTypes.kt
    ├── FileComparator.kt
    ├── history/
    │   ├── History.kt
    │   ├── HistoryFilter.kt
    │   ├── HistoryListener.kt
    │   └── HistoryStore.kt
    └── ui/
        ├── Dlg.kt
        ├── LoadingDialog.kt
        ├── MenuItemAdapter.kt
        ├── TaskListener.kt
        ├── ContextMenuUtils.kt
        └── ViewFilesContextMenuUtils.kt
```

## Использование

```kotlin
// Открыть файловый менеджер
val intent = Intent(this, FileChooserActivity::class.java)
intent.putExtra(FileChooserActivity._FilterMode, IFileProvider.FilterMode.FilesOnly)
startActivityForResult(intent, SELECT_FILE_REQUEST)

// Обработка результата
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == SELECT_FILE_REQUEST && resultCode == RESULT_OK) {
        val files = data?.getParcelableArrayListExtra<IFile>(FileChooserActivity._Results)
        // Обработка выбранных файлов
    }
}
```

## Ключи Intent

- `_Rootpath` - корневой путь (IFile)
- `_FileProviderClass` - класс провайдера файлов
- `_FilterMode` - режим фильтрации (FilesOnly, DirectoriesOnly, FilesAndDirectories)
- `_MultiSelection` - множественный выбор
- `_SaveDialog` - режим сохранения
- `_DefaultFilename` - имя файла по умолчанию
- `_DisplayHiddenFiles` - показывать скрытые файлы
- `_MaxFileCount` - максимальное количество файлов
- `_RegexFilenameFilter` - regex фильтр имён файлов
- `_DoubleTapToChooseFiles` - двойной тап для выбора
- `_Results` - результаты выбора (ArrayList<IFile>)
- `_FolderPath` - путь к папке
