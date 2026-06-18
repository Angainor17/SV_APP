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

## Правила разработки

- **Все строки, видимые пользователю, должны быть вынесены в строковые ресурсы** (`strings.xml`)
- Использовать `stringResource()` для получения строк в Compose
- Для строк с параметрами использовать формат `%s` в ресурсах и передавать параметры в `stringResource(R.string.key, param)`

## Примечания

Модуль минимален и может быть расширен для:
- Отображения статей Wiki
- Поиска по Wiki
- Закладок и истории