# AndroidFileChooser Module

Компонент для выбора файлов.

## Обзор

Модуль `androidFileChooser` предоставляет Activity для выбора файлов в файловой системе устройства.

## Основные компоненты

### FileChooserActivity
Activity для выбора файлов:

```java
Intent intent = new Intent(context, FileChooserActivity.class);
intent.putExtra(FileChooserActivity.EXTRA_ROOT_DIRECTORY, rootDir);
intent.putExtra(FileChooserActivity.EXTRA_MODE, mode);
startActivityForResult(intent, REQUEST_CODE);
```

### IFile
Интерфейс для абстракции над файлами:

```java
public interface IFile {
    String getName();
    String getPath();
    boolean isDirectory();
    long length();
}
```

### IFileAdapter
Адаптер для отображения списка файлов.

### IFileFilter
Фильтр для файлов:

```java
public interface IFileFilter {
    boolean accept(IFile file);
}
```

### IFileDataModel
Модель данных для файлов.

## Настройки (Prefs)

### DisplayPrefs
Настройки отображения:

```java
DisplayPrefs.setShowHiddenFiles(context, true);
DisplayPrefs.setSortOrder(context, SortOrder.NAME);
```

### Prefs
Общие настройки файлового менеджера.

## Утилиты

### FileUtils
Утилиты для работы с файлами:

```java
FileUtils.getFileExtension(file);
FileUtils.getFileNameWithoutExtension(file);
```

### Utils
Общие утилиты.

## Структура файлов

```
androidFileChooser/src/main/java/group/pals/android/lib/ui/filechooser/
├── FileChooserActivity.java
├── IFileAdapter.java
├── IFileDataModel.java
├── IFile.java
├── io/
│   └── IFileFilter.java
├── prefs/
│   ├── DisplayPrefs.java
│   └── Prefs.java
└── utils/
    ├── FileUtils.java
    ├── Utils.java
    └── E.java
```

## Использование

```java
// Открыть файловый менеджер
Intent intent = new Intent(this, FileChooserActivity.class);
intent.putExtra(FileChooserActivity.EXTRA_MODE, Mode.File);
startActivityForResult(intent, SELECT_FILE_REQUEST);

// Обработка результата
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == SELECT_FILE_REQUEST && resultCode == RESULT_OK) {
        Uri fileUri = data.getData();
        // Обработка выбранного файла
    }
}
```
