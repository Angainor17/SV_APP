# Документация API MediaWiki для svremya.su

## Базовый URL
```
https://svremya.su/api.php
```

---

## 1. Поиск статей

### Endpoint: `action=query&list=search`

**Назначение**: Поиск статей по тексту или заголовкам

### Параметры запроса

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `action` | string | ✅ | `query` |
| `list` | string | ✅ | `search` |
| `srsearch` | string | ✅ | Поисковый запрос (URL-encoded) |
| `srwhat` | string | ❌ | `text` (по умолчанию), `title` (по заголовкам), `nearmatch` (точное совпадение) |
| `srlimit` | int | ❌ | Максимальное количество результатов (по умолчанию 10) |
| `format` | string | ✅ | `json` |
| `utf8` | string | ❌ | Пустая строка для включения UTF-8 |

### Примеры запросов

#### Поиск по тексту
```
GET https://svremya.su/api.php?action=query&list=search&srsearch=маркс&format=json&utf8=
```

#### Точное совпадение по заголовку (рекомендуется)
```
GET https://svremya.su/api.php?action=query&list=search&srsearch=Конечное&srwhat=nearmatch&format=json&utf8=
```

### Формат ответа

#### Успешный поиск (найдены результаты)
```json
{
  "batchcomplete": "",
  "query": {
    "searchinfo": {
      "totalhits": 1
    },
    "search": [
      {
        "ns": 0,
        "title": "Конечное",
        "pageid": 223,
        "size": 392,
        "wordcount": 5,
        "snippet": "<span class=\"searchmatch\">Конечное</span> - это...",
        "timestamp": "2025-12-29T10:59:28Z"
      }
    ]
  }
}
```

#### Ничего не найдено
```json
{
  "batchcomplete": "",
  "query": {
    "searchinfo": {
      "totalhits": 0
    },
    "search": []
  }
}
```

### Поля ответа

| Поле | Тип | Описание |
|------|-----|----------|
| `totalhits` | int | Общее количество найденных результатов |
| `ns` | int | Namespace (0 = основное пространство) |
| `title` | string | Заголовок статьи |
| `pageid` | int | ID страницы |
| `size` | int | Размер в байтах |
| `wordcount` | int | Количество слов |
| `snippet` | string | Фрагмент текста с подсветкой |
| `timestamp` | string | Дата последнего изменения (ISO 8601) |

---

## 2. Получение содержимого страницы

### Endpoint: `action=parse`

**Назначение**: Получение HTML-содержимого страницы с обработанными ссылками

### Параметры запроса

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `action` | string | ✅ | `parse` |
| `page` | string | ✅ | Заголовок страницы (URL-encoded) |
| `prop` | string | ✅ | `text` (HTML), `links` (ссылки), `displaytitle` (заголовок) |
| `format` | string | ✅ | `json` |

### Пример запроса
```
GET https://svremya.su/api.php?action=parse&page=Конечное&prop=text|links|displaytitle&format=json
```

### Формат ответа

#### Успешный ответ
```json
{
  "parse": {
    "title": "Конечное",
    "pageid": 223,
    "text": {
      "*": "<div class=\"mw-content-ltr mw-parser-output\" lang=\"ru\" dir=\"ltr\"><p>Конечное - это <a href=\"/Нечто\" title=\"Нечто\">нечто</a>, взятое со своей имманентной <a href=\"/Граница\" title=\"Граница\">границей</a>...</p></div>"
    },
    "links": [
      {
        "ns": 0,
        "exists": "",
        "*": "Граница"
      },
      {
        "ns": 0,
        "exists": "",
        "*": "Нечто"
      }
    ],
    "displaytitle": "<span class=\"mw-page-title-main\">Конечное</span>"
  }
}
```

#### Страница не существует
```json
{
  "error": {
    "code": "missingtitle",
    "info": "The page you specified doesn't exist."
  }
}
```

### Поля ответа

| Поле | Тип | Описание |
|------|-----|----------|
| `title` | string | Заголовок страницы |
| `pageid` | int | ID страницы |
| `text["*"]` | string | HTML-содержимое |
| `links` | array | Массив ссылок на другие статьи |
| `links[].ns` | int | Namespace ссылки |
| `links[].exists` | string | Пустая строка, если статья существует |
| `links[].*` | string | Заголовок связанной статьи |

---

## 3. OpenSearch (автодополнение)

### Endpoint: `action=opensearch`

**Назначение**: Быстрый поиск для автодополнения

### Параметры запроса

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| `action` | string | ✅ | `opensearch` |
| `search` | string | ✅ | Поисковый запрос |
| `limit` | int | ❌ | Максимальное количество результатов |
| `format` | string | ✅ | `json` |

### Пример запроса
```
GET https://svremya.su/api.php?action=opensearch&search=конеч&limit=5&format=json
```

### Формат ответа
```json
[
  "конеч",
  ["Конечное"],
  [""],
  ["https://svremya.su/Конечное"]
]
```

### Структура ответа (массив из 4 элементов)
1. `[0]` - Поисковый запрос
2. `[1]` - Массив заголовков
3. `[2]` - Массив описаний (обычно пустые)
4. `[3]` - Массив URL

---

## 4. Рекомендуемые стратегии поиска

### Стратегия 1: Поиск с переходом к статье
1. Использовать `srwhat=nearmatch` для точного совпадения
2. Если найдено → загрузить статью через `action=parse`
3. Если не найдено → показать "Ничего не найдено"

### Стратегия 2: Поиск с выбором из списка
1. Использовать обычный поиск (`srwhat=text`)
2. Показать список результатов
3. При выборе → загрузить статью

### Рекомендуемая стратегия для данного проекта
Использовать **Стратегию 1** с fallback на `opensearch` для автодополнения при вводе.

---

## 5. Формат HTML-ссылок

### Структура ссылки в HTML
```html
<a href="/%D0%9D%D0%B5%D1%87%D1%82%D0%BE" title="Нечто">нечто</a>
```

### Парсинг ссылки
- `href` - URL-encoded путь к статье (без домена)
- `title` - Заголовок статьи (tooltip)
- Текст между тегами - отображаемый текст

### Пример обработки
```kotlin
// Регулярное выражение для поиска ссылок
val linkRegex = """<a href="/([^"]+)" title="([^"]+)">([^<]+)</a>""".toRegex()

// Извлечение
val matches = linkRegex.findAll(htmlContent)
matches.forEach { match ->
    val encodedPath = match.groupValues[1]  // %D0%9D%D0%B5%D1%87%D1%82%D0%BE
    val title = match.groupValues[2]         // Нечто
    val displayText = match.groupValues[3]   // нечто
}
```

---

## 6. Обработка ошибок

### Коды ошибок

| Код | Описание | Действие |
|-----|----------|----------|
| `missingtitle` | Страница не существует | Показать "Ничего не найдено" |
| `invalidtitle` | Некорректный заголовок | Показать ошибку ввода |
| HTTP 5xx | Ошибка сервера | Показать ошибку с кнопкой повтора |
| Timeout | Превышено время ожидания | Показать ошибку сети |

### Пример обработки
```kotlin
when {
    response.isSuccessful -> {
        // Обработка успешного ответа
    }
    response.code() == 404 -> {
        // Страница не найдена
        UiWikiState.NotFound
    }
    else -> {
        // Другие ошибки
        UiWikiState.Error(response.message())
    }
}
```

---

## 7. Примеры полного цикла

### Пример 1: Поиск статьи "Конечное"
```
1. GET /api.php?action=query&list=search&srsearch=Конечное&srwhat=nearmatch&format=json
   → Найдена статья с title="Конечное", pageid=223

2. GET /api.php?action=parse&page=Конечное&prop=text|links&format=json
   → Получен HTML и список ссылок

3. Отображение статьи с кликабельными ссылками
```

### Пример 2: Переход по ссылке
```
1. Пользователь кликает на ссылку "нечто" (href="/Нечто")

2. GET /api.php?action=parse&page=Нечто&prop=text|links&format=json
   → Получена новая статья

3. Обновление UI без изменения поля поиска
```

---

## 8. Примечания

- API не требует авторизации
- UTF-8 кодировка обязательна для русского текста
- Рекомендуется добавить `User-Agent` заголовок
- Нет ограничений на количество запросов (публичный API)
