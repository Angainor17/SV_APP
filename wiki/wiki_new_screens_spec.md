# ТЗ: Новые экраны Wiki-модуля

**Статус:** В разработке
**Дата:** 2026-06-18
**Версия:** 1.0

---

## 1. Обзор

Документ описывает реализацию новых экранов в Wiki-модуле:
- Иконка избранного на главном экране
- Экран "Избранное" (список избранных статей)
- Экран "Статья" (просмотр статьи)

---

## 2. Архитектурные требования

### 2.1 Паттерны и технологии
- **Архитектура:** MVVM + Clean Architecture
- **DI:** Hilt
- **UI:** Jetpack Compose + Material 3
- **Навигация:** Navigation Compose

### 2.2 Структура экранов

```
RootWiki (главный экран)
├── WikiSearchBar + иконка избранного
├── SearchSuggestions
├── HistoryList / ArticleView
│
├── FavoritesScreen (экран избранного) ← новый
│   ├── TopAppBar с заголовком и корзиной
│   └── FavoritesList
│
└── ArticleScreen (экран статьи) ← новый
    ├── TopAppBar с заголовком и действиями
    └── ArticleContent
```

---

## 3. Экран RootWiki - изменения

### 3.1 Иконка избранного

**Расположение:** Справа от поля поиска

**Требования:**
- Иконка: сердечко (Icons.Default.Favorite)
- Отображается только при наличии избранных статей
- При нажатии — переход на экран "Избранное"
- Анимация исчезновения при очистке избранного

**Реализация:**
```kotlin
// Состояние
val hasFavorites by viewModel.hasFavorites.collectAsStateWithLifecycle()

// UI
AnimatedVisibility(visible = hasFavorites) {
    IconButton(onClick = { navController.navigate("favorites") }) {
        Icon(Icons.Default.Favorite, ...)
    }
}
```

---

## 4. Экран "Избранное" (FavoritesScreen)

### 4.1 Компоненты UI

#### TopAppBar
```
┌─────────────────────────────────────┐
│ ←  Избранное                    🗑  │
└─────────────────────────────────────┘
```

- **Заголовок:** "Избранное"
- **Стрелка назад:** возврат на RootWiki
- **Иконка корзины:** очистка избранного (только при наличии элементов)

#### AlertDialog (очистка)
```
┌─────────────────────────────────────┐
│   Вы точно хотите очистить          │
│   избранное?                        │
│                                     │
│          [Нет]    [Да]              │
└─────────────────────────────────────┘
```

#### Список избранного
```
┌─────────────────────────────────────┐
│ 📄 Маркс, Карл                      │
│─────────────────────────────────────│
│ 📄 Марксизм                         │
│─────────────────────────────────────│
│ 📄 Стоимость                        │
└─────────────────────────────────────┘
```

- Пункт списка = название статьи
- При клике — переход на ArticleScreen

### 4.2 Состояния экрана

```kotlin
sealed class FavoritesState {
    object Loading : FavoritesState()
    data class Content(val favorites: List<String>) : FavoritesState()
    object Empty : FavoritesState()
}
```

### 4.3 Действия

| Действие | Описание |
|----------|----------|
| `OnBackClick` | Возврат на предыдущий экран |
| `OnClearClick` | Показать диалог очистки |
| `OnClearConfirm` | Очистить избранное и закрыть экран |
| `OnItemClick(title)` | Переход на экран статьи |

---

## 5. Экран "Статья" (ArticleScreen)

### 5.1 Компоненты UI

#### TopAppBar
```
┌─────────────────────────────────────┐
│ ←  Марксизм            ❤️  🔗      │
└─────────────────────────────────────┘
```

- **Заголовок:** название статьи
- **Стрелка назад:** возврат
- **Иконка избранного:** добавить/удалить из избранного
- **Иконка внешней ссылки:** открыть в браузере

#### Контент статьи
- Текст с вертикальным скроллом
- Возможность выделения текста (SelectionContainer)
- Кликабельные внутренние ссылки
- Кликабельные внешние ссылки (другой цвет)

### 5.2 Модель данных

```kotlin
data class ArticleScreenState(
    val title: String,
    val content: String,
    val links: List<UiWikiLink>,
    val externalLinks: List<UiExternalLink>,
    val articleUrl: String,        // URL для открытия в браузере
    val isFavorite: Boolean,
)
```

### 5.3 Действия

| Действие | Описание |
|----------|----------|
| `OnBackClick` | Возврат на предыдущий экран |
| `OnFavoriteClick` | Добавить/удалить из избранного |
| `OnExternalLinkClick` | Открыть URL в браузере |
| `OnInternalLinkClick(title)` | Загрузить другую статью |

---

## 6. Изменения в БД

### 6.1 Таблица favorites

| Поле | Тип | Описание |
|------|-----|----------|
| title | String (PK) | Название статьи |
| content | String | HTML-содержимое |
| links | String (JSON) | Внутренние ссылки |
| externalLinks | String (JSON) | Внешние ссылки |
| articleUrl | String | URL статьи на сайте |
| savedAt | Long | Дата сохранения |

### 6.2 Версия БД
- Сбросить до версии 1
- Без миграции (проект не опубликован)

---

## 7. Переиспользование компонентов

### 7.1 Общие компоненты

| Компонент | Расположение | Используется в |
|-----------|--------------|----------------|
| `ArticleContent` | `commonui/` или `wiki/ui/` | RootWiki, ArticleScreen |
| `WikiSearchBar` | `wiki/ui/` | RootWiki |
| `SearchSuggestions` | `wiki/ui/` | RootWiki |
| `HistoryList` | `wiki/ui/` | RootWiki |

### 7.2 Общие UseCase

| UseCase | Используется в |
|---------|----------------|
| `GetArticleUseCase` | RootWiki, ArticleScreen |
| `AddFavoriteUseCase` | RootWiki, ArticleScreen |
| `RemoveFavoriteUseCase` | RootWiki, ArticleScreen, FavoritesScreen |
| `IsFavoriteUseCase` | RootWiki, ArticleScreen |
| `GetFavoritesUseCase` | RootWiki (hasFavorites), FavoritesScreen |
| `ClearFavoritesUseCase` | FavoritesScreen (новый) |

---

## 8. Навигация

### 8.1 Граф навигации

```kotlin
NavHost(startDestination = "wiki") {
    composable("wiki") {
        RootWiki(
            onNavigateToFavorites = { navController.navigate("favorites") },
            onNavigateToArticle = { title -> navController.navigate("article/$title") },
        )
    }
    composable("favorites") {
        FavoritesScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToArticle = { title -> navController.navigate("article/$title") },
            onFavoritesCleared = { navController.popBackStack() },
        )
    }
    composable("article/{title}") {
        ArticleScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
```

### 8.2 Изменения в RootWiki

- При клике на историю → ArticleScreen (вместо загрузки в блок)
- Иконка избранного → FavoritesScreen

---

## 9. Анимации

| Элемент | Анимация | Длительность |
|---------|----------|--------------|
| Иконка избранного на RootWiki | `AnimatedVisibility` (fade + scale) | 300мс |
| Пункт в списке избранного при удалении | `animateItemPlacement` | 200мс |
| Подсказки поиска | `expandVertically` / `shrinkVertically` | 300мс / 200мс |

---

## 10. Этапы реализации

1. **Подготовка**
   - Создать общие компоненты
   - Обновить БД (сброс версии)
   - Добавить ClearFavoritesUseCase

2. **Иконка избранного**
   - Добавить hasFavorites в ViewModel
   - Добавить иконку с AnimatedVisibility в RootWiki

3. **Экран "Избранное"**
   - Создать FavoritesViewModel
   - Создать FavoritesScreen
   - Реализовать AlertDialog

4. **Экран "Статья"**
   - Создать ArticleViewModel
   - Создать ArticleScreen
   - Добавить URL в БД

5. **Навигация**
   - Настроить Navigation Compose
   - Обновить переходы в RootWiki

6. **Тестирование и документация**
   - Проверить все сценарии
   - Обновить wiki_for_CLAUDE_fix.md

---

## 11. Тестовые сценарии

### 11.1 Иконка избранного
- [ ] Иконка не отображается при пустом избранном
- [ ] Иконка появляется при добавлении первой статьи
- [ ] Иконка исчезает при очистке избранного
- [ ] Переход на экран избранного работает

### 11.2 Экран "Избранное"
- [ ] Отображается список избранных статей
- [ ] AlertDialog показывается при нажатии на корзину
- [ ] "Нет" закрывает диалог
- [ ] "Да" очищает избранное и закрывает экран
- [ ] Переход на статью работает

### 11.3 Экран "Статья"
- [ ] Статья загружается корректно
- [ ] Возврат на предыдущий экран работает
- [ ] Иконка избранного переключается
- [ ] При возврате на "Избранное" список обновляется
- [ ] Внешняя ссылка открывается в браузере

### 11.4 Навигация
- [ ] История → ArticleScreen работает
- [ ] Все возвраты работают корректно
