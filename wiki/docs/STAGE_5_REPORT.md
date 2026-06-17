# Этап 5: Завершён

## Выполненные задачи

### 1. Созданы UI-модели

**UiWikiState.kt:**
- `Initial` - начальное состояние (показываем историю)
- `Loading` - загрузка
- `Content` - статья найдена
- `NotFound` - ничего не найдено
- `Error` - ошибка

**UiWikiArticle.kt:**
- `UiWikiArticle` - модель статьи
- `UiWikiLink` - модель ссылки

### 2. Созданы Actions

**WikiActions.kt:**
- `OnSearch(query)` - поиск статьи
- `OnLinkClick(title)` - клик по ссылке
- `OnAddFavorite(title)` - добавить в избранное
- `OnRemoveFavorite(title)` - удалить из избранного
- `OnHistoryItemClick(title)` - клик по истории
- `OnClearHistory` - очистить историю
- `OnRetryClick` - повторить
- `OnCloseArticle` - закрыть статью

### 3. Созданы Effects

**WikiOneTimeEffect.kt:**
- `ShowErrorSnackBar` - показать ошибку
- `ShowAddedToFavorites` - добавлено в избранное
- `ShowRemovedFromFavorites` - удалено из избранного

### 4. Создан Mapper

**UiWikiMapper.kt:**
- Маппинг `WikiArticle` → `UiWikiArticle`
- Маппинг `WikiLink` → `UiWikiLink`

### 5. Создана ViewModel

**RootWikiViewModel.kt:**
- Управление состоянием через `StateFlow`
- Обработка всех Actions
- Интеграция со всеми Use Cases
- Отправка OneTimeEffect

## Проверка сборки

```bash
./gradlew :wiki:assembleDebug
```

**Результат**: ✅ BUILD SUCCESSFUL

## Структура модуля wiki

```
wiki/src/main/java/su/sv/wiki/
├── data/
│   ├── api/
│   ├── local/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   └── root/
│       ├── mapper/
│       │   └── UiWikiMapper.kt
│       ├── model/
│       │   ├── UiWikiArticle.kt
│       │   └── UiWikiState.kt
│       └── viewmodel/
│           ├── RootWikiViewModel.kt
│           ├── actions/
│           │   ├── WikiActions.kt
│           │   └── WikiActionsHandler.kt
│           └── effects/
│               └── WikiOneTimeEffect.kt
└── di/
```

---

## Запрос на проверку

✅ Этап 5 завершён. Сборка успешна.

**Готов к переходу на Этап 6: UI-компоненты (Compose)**
