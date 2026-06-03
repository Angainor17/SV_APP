# Wiki Module

Модуль Wiki-страницы.

## Обзор

Модуль `wiki` отображает Wiki-контент (Википедию или внутреннюю wiki).

## Основные компоненты

### RootWiki
Главный экран Wiki:

```kotlin
@Composable
fun RootWiki(
    navController: NavHostController
)
```

## Структура файлов

```
wiki/src/main/java/su/sv/wiki/
└── root/
    └── RootWiki.kt
```

## Примечания

Модуль минимален и может быть расширен для:
- Отображения статей Wiki
- Поиска по Wiki
- Закладок и истории

Экран пока не используется в приложении