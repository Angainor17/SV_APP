---
name: compose-optimizations
description: Оптимизации Jetpack Compose в SV APP (2026-06-25)
metadata: 
  node_type: memory
  type: project
  originSessionId: 7eb6efa8-871b-4688-84c1-32e3cf8fb9e2
---

# Compose Optimizations

Оптимизации производительности Jetpack Compose, применённые 2026-06-25.

---

## 1. BookList.kt — `items()` вместо `forEach`

**Файл:** `books/src/main/java/su/sv/books/catalog/presentation/root/ui/BookList.kt`

**Было:**
```kotlin
state.filteredBooks.forEach { book ->
    item(key = book.id) {
        BookItem(book, actions)
    }
}
```

**Стало:**
```kotlin
items(
    items = state.filteredBooks,
    key = { it.id }
) { book ->
    BookItem(book, actions)
}
```

**Причина:** Использование `items()` с `key` позволяет LazyGrid более эффективно управлять рекомпозицией и переиспользовать элементы.

**Потенциальный баг:** Если элементы списка не отображаются или дублируются — проверить уникальность `book.id`.

---

## 2. InfoContent.kt — `items()` с данными вместо индекса

**Файл:** `info/src/main/java/su/sv/info/rootinfo/ui/InfoContent.kt`

**Было:**
```kotlin
items(state.items.size) {
    InfoItem(state.items[it])
}
```

**Стало:**
```kotlin
items(
    items = state.items,
    key = { it.url }
) { item ->
    InfoItem(item)
}
```

**Причина:** Индексный доступ без ключа вызывает полную рекомпозицию при любых изменениях списка. Ключи позволяют Compose отслеживать элементы. Используется `url` как уникальный ключ, так как `id` отсутствует в модели.

**Потенциальный баг:** Если элементы не отображаются — проверить уникальность `url` в `UiLinkItem`. Если URL не уникален, элементы могут пропадать.

---

## 3. BottomNavigationUi.kt — derivedStateOf для navigationSelectedItem

**Файл:** `main/src/main/java/su/sv/main/bottomnav/BottomNavigationUi.kt`

**Было:**
```kotlin
val navigationSelectedItem = remember(currentRoute) {
    when (currentRoute) { ... }
}
```

**Стало:**
```kotlin
val navigationSelectedItem by remember {
    derivedStateOf {
        when (currentRoute) { ... }
    }
}
```

**Причина:** `derivedStateOf` оптимизирует пересчёт значения — recomposition происходит только когда результат действительно изменился.

**Потенциальный баг:** Если не подсвечивается правильный таб — проверить логику маппинга route → index.

---

## 4. BottomNavigationUi.kt — rememberBottomNavigationItems()

**Файл:** `main/src/main/java/su/sv/main/bottomnav/BottomNavigationUi.kt`

**Было:**
```kotlin
@Composable
fun bottomNavigationItems(): List<BottomNavigationItem> {
    return listOf(...)
}
```

**Стало:**
```kotlin
@Composable
private fun rememberBottomNavigationItems(): List<BottomNavigationItem> {
    return remember { listOf(...) }
}
```

**Причина:** Мемоизация списка避免了 пересоздания при каждой рекомпозиции.

**Потенциальный баг:** Если изменились строки ресурсов (локализация) — они не обновятся динамически. В текущей реализации строки захардкожены. При необходимости вернуть `stringResource()`.

---

## 5. NewsList.kt — добавлен contentType

**Файл:** `news/src/main/java/su/sv/news/presentation/root/ui/NewsList.kt`

**Было:**
```kotlin
items(
    count = lazyPagingItems.itemCount,
    key = lazyPagingItems.itemKey { it.id },
) { index ->
    val item = lazyPagingItems[index]
    if (item != null) { ... } else { ... }
}
```

**Стало:**
```kotlin
items(
    count = lazyPagingItems.itemCount,
    key = lazyPagingItems.itemKey { it.id },
    contentType = { "news_item" }
) { index ->
    lazyPagingItems[index]?.let { ... } ?: MessagePlaceholder()
}
```

**Причина:** `contentType` позволяет Compose оптимизировать переиспользование view-холдеров при скролле.

**Потенциальный баг:** Если есть разные типы элементов в списке — нужно добавить разные contentType.

---

## 6. RootNews.kt — упрощение логики состояний

**Файл:** `news/src/main/java/su/sv/news/presentation/root/ui/RootNews.kt`

**Было:**
```kotlin
when (loadState) {
    LoadState.Loading if !hasItems -> { ... }
    is LoadState.Error if !hasItems -> { ... }
    else -> {
        if (hasItems) { ... } else { ... }
    }
}
```

**Стало:**
```kotlin
when {
    loadState is LoadState.Loading && !hasItems -> { ... }
    loadState is LoadState.Error && !hasItems -> { ... }
    hasItems -> { ... }
    else -> { ... }
}
```

**Причина:** Упрощение читаемости и избежание вложенных условий.

**Потенциальный баг:** Если отображается неправильное состояние (загрузка вместо контента) — проверить порядок условий в `when`.

---

## Откат изменений

При обнаружении функционального бага:

1. **BookList/InfoContent:** вернуть `forEach` + `item()` или `items(size)`
2. **BottomNavigation:** убрать `derivedStateOf`, вернуть `remember(currentRoute)`
3. **NewsList:** убрать `contentType`
4. **RootNews:** вернуть вложенный `when`

---

## 7. contentWindowInsets для вложенных Scaffold

**Файлы:**
- `books/.../RootBooksCatalog.kt`
- `news/.../RootNews.kt`
- `info/.../RootInfo.kt`
- `wiki/.../RootWiki.kt`

**Изменение:**
```kotlin
Scaffold(
    contentWindowInsets = WindowInsets(0.dp),
    // ...
)
```

**Причина:** Внешний Scaffold в BottomNavigation уже добавляет отступы для bottomBar и статус-бара. Вложенные Scaffold по умолчанию также добавляют `contentWindowInsets`, что приводит к двойным отступам (лишний отступ между контентом и bottom navigation bar).

**Потенциальный баг:** Если убрать внешний Scaffold или изменить структуру навигации — нужно будет вернуть `contentWindowInsets` по умолчанию.

---

## Откат изменений

При обнаружении функционального бага:

1. **BookList/InfoContent:** вернуть `forEach` + `item()` или `items(size)`
2. **BottomNavigation:** убрать `derivedStateOf`, вернуть `remember(currentRoute)`
3. **NewsList:** убрать `contentType`
4. **RootNews:** вернуть вложенный `when`

---

## Связанные файлы

- [[design-system]] — дизайн-система SV APP
- [[mock-system]] — система моков
