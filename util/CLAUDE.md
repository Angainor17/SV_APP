# Util Module

Общие утилиты приложения.

## Обзор

Модуль `util` содержит вспомогательные классы и утилиты, используемые в разных модулях приложения.

## Основные классы

### ComparisonUtil
Утилиты для сравнения объектов.

### Pair
Класс пары значений (аналог стандартного Kotlin Pair, но с дополнительным функционалом).

### NaturalOrderComparator
Компаратор для естественной сортировки строк:

```kotlin
NaturalOrderComparator.compare("file1", "file10") // -1 (file1 < file10)
```

Полезен для сортировки имён файлов, где числовые части должны сравниваться как числа.

### Boolean3
Трёхзначная логика (True, False, Unknown):

```kotlin
enum class Boolean3 {
    TRUE,
    FALSE,
    UNKNOWN
}
```

Используется когда результат может быть неопределённым.

## Структура файлов

```
util/src/main/java/org/fbreader/util/
├── Boolean3.kt
├── ComparisonUtil.kt
├── NaturalOrderComparator.kt
└── Pair.kt
```

## Примечания

Утилиты находятся в пакете `org.fbreader.util` так как изначально были частью библиотеки FBReader.
