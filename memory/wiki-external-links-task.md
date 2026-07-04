---
name: wiki-external-links-task
description: Задача по реализации поддержки внешних ссылок в Wiki-модуле
metadata: 
  node_type: memory
  type: project
  originSessionId: f4ae719b-79ad-4a64-99ab-a83fc1bc08d7
---

# Поддержка внешних ссылок в Wiki-модуле

**Статус**: Реализовано
**Приоритет**: Средний
**Связано с**: [[wiki-module-task]]

## Проблема

При поиске статьи "Стоимость" в конце текста появляется блок:
> "Смотрите также видео: В. И. Галко. Что такое стоимость?"

На исходном сайте (svremya.su) этот текст содержит ссылку на внешнее видео. В приложении ссылка не работает.

## Цель

Сделать внешние ссылки в тексте статей кликабельными с открытием во внешнем браузере.

## Анализ

### Текущая реализация
- HTML-контент приходит с API (`action=parse`)
- В `ArticleView.kt` HTML очищается от тегов, остаются только внутренние wiki-ссылки
- Внешние ссылки теряются при обработке

### Результаты исследования API (выполнено)

**Запрос**: `api.php?action=parse&page=Стоимость&prop=text|links|extlinks&format=json`

**Результат**:
- API не поддерживает `extlinks` property (warning: "Unrecognized value for parameter \"prop\": extlinks")
- Внешние ссылки присутствуют только в HTML-контенте

**Структура внешней ссылки в HTML**:
```html
<a rel="nofollow" class="external text" href="https://vk.com/video-206226873_456239852">
    В. И. Галко. Что такое стоимость?
</a>
```

**Признаки внешней ссылки**:
- `class="external text"` или `class="external"`
- `rel="nofollow"`
- `href` начинается с `http://` или `https://` (не с `/`)

**Контекст** (статья "Стоимость"):
```html
<table class="wikitable">
<tbody><tr>
<th>Смотрите также видео: <a rel="nofollow" class="external text" href="...">В. И. Галко. Что такое стоимость?</a>
</th></tr>
</tbody></table>
```

## План реализации

### Этап 1: Обновление моделей данных

**Файлы для изменения**:
- `domain/model/WikiArticle.kt`
- `presentation/root/model/UiWikiArticle.kt`
- `presentation/root/mapper/UiWikiMapper.kt`

**Новые модели**:
```kotlin
// domain/model/WikiArticle.kt
data class WikiExternalLink(
    val text: String,        // Отображаемый текст
    val url: String,         // URL ссылки
)

data class WikiArticle(
    val title: String,
    val pageId: Int,
    val content: String,
    val links: List<WikiLink>,
    val externalLinks: List<WikiExternalLink>,  // ДОБАВИТЬ
)
```

```kotlin
// presentation/root/model/UiWikiArticle.kt
data class UiExternalLink(
    val text: String,
    val url: String,
)

data class UiWikiArticle(
    val title: String,
    val content: String,
    val links: List<UiWikiLink>,
    val externalLinks: List<UiExternalLink>,  // ДОБАВИТЬ
)
```

### Этап 2: Парсинг внешних ссылок из HTML

**Файлы для изменения**:
- `data/repository/WikiRepositoryImpl.kt`

**Алгоритм парсинга**:
1. Использовать Regex для поиска `<a>` тегов с `class="external"`
2. Извлечь `href` и текст ссылки
3. Добавить в модель `WikiArticle`

**Пример реализации**:
```kotlin
private fun parseExternalLinks(html: String): List<WikiExternalLink> {
    val regex = """<a[^>]*class="external[^"]*"[^>]*href="([^"]+)"[^>]*>([^<]+)</a>""".toRegex()
    return regex.findAll(html).map { match ->
        WikiExternalLink(
            url = match.groupValues[1],
            text = match.groupValues[2],
        )
    }.toList()
}
```

### Этап 3: Отображение в UI

**Файлы для изменения**:
- `presentation/root/ui/ArticleView.kt`
- `root/RootWiki.kt`

**Изменения в `buildAnnotatedContent`**:
1. При очистке HTML сохранять текст внешних ссылок
2. Использовать `LinkAnnotation.Url` для внешних ссылок
3. При клике открывать `Intent.ACTION_VIEW`

**Пример**:
```kotlin
withLink(LinkAnnotation.Url(link.url)) {
    withStyle(style = SpanStyle(
        color = MaterialTheme.colorScheme.tertiary,
        textDecoration = TextDecoration.Underline,
    )) {
        append(link.text)
    }
}
```

**Открытие внешней ссылки**:
```kotlin
// В ArticleView добавить параметр
onExternalLinkClick: (String) -> Unit

// В RootWiki
val context = LocalContext.current
ArticleView(
    ...
    onExternalLinkClick = { url ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
)
```

### Этап 4: Визуальное отличие внешних ссылок

**Идеи**:
- Другой цвет (tertiary вместо primary)
- Иконка внешней ссылки после текста (опционально)

### Этап 5: Тестирование

- [ ] Статья "Стоимость" - ссылка на видео VK открывается
- [ ] Внутренние wiki-ссылки продолжают работать
- [ ] Текст статьи корректно отображается
- [ ] Вертикальный скролл и выделение текста работают

## Технические детали

### Зависимости
- Стандартные библиотеки Kotlin (Regex)
- Android Intent API
- Jetpack Compose Text API

### Риски
- Позиция ссылки в тексте может смещаться при очистке HTML
- Необходимо обрабатывать HTML-сущности (`&amp;`, `&quot;` и т.д.)

## Оценка трудозатрат

| Этап | Время |
|------|-------|
| Модели данных | 15 мин |
| Парсинг HTML | 30 мин |
| UI изменения | 30 мин |
| Тестирование | 15 мин |
| **Итого** | ~1.5 часа |

**Why:** Пользователи не могут переходить по внешним ссылкам (видео, источники), что ограничивает функциональность Wiki-модуля.

**How to apply:** Выполнить этапы последовательно. После каждого этапа проверять компиляцию и базовую функциональность.
