# Заметки по миграции BookReader - Этапы 1-4

## Текущий статус

**Все основные этапы (1-4) завершены. Осталось тестирование (Этап 5).**

| Этап | Статус | Дата |
|------|--------|------|
| 1. Подготовка | ✅ | 2026-06-19 |
| 2. Миграция ReaderFragment | ✅ | 2026-06-19 |
| 3. Интеграция | ✅ | 2026-06-20 |
| 4. Settings | ✅ | 2026-06-20 |
| 5. Очистка и тестирование | ⏳ | - |

---

## Что было сделано

### Этап 1: Подготовка

**Созданные файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/app/BookReaderInitializer.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderScreen.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderState.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderActions.kt`

**Изменённые файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/app/BookApplication.kt` - теперь делегирует к BookReaderInitializer
- `bookreader/build.gradle.kts` - добавлены зависимости:
  - `project(":commonui")` для SVAPPTheme
  - `libs.modo.compose` для навигации
  - `libs.androidx.hilt.navigation.compose` для hiltViewModel
  - `id("kotlin-parcelize")` плагин

### Этап 2: Миграция ReaderFragment

**Созданные файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderViewModel.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/ReaderTopBar.kt`

**Обновлённые файлы:**
- `ReaderContent.kt` - интегрирован FBReaderView через AndroidView

**Ключевые особенности:**
- FBReaderView интегрируется через `AndroidView`
- Обработка батареи через `BroadcastReceiver`
- Обработка клавиш громкости для навигации
- Сохранение позиции через `DisposableEffect`

### Этап 3: Интеграция

**Удалённые файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/activities/BookReaderMainActivity.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/activities/FullscreenActivity.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/fragments/ReaderFragment.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/fragments/LibraryFragment.kt`

**Изменённые файлы:**
- `books/src/main/java/su/sv/books/catalog/presentation/detail/ui/BookDetailUi.kt`
  - Убран Intent к BookReaderMainActivity
  - Добавлен переход к ReaderScreen через Modo
- `books/src/main/java/su/sv/books/catalog/presentation/root/ui/RootBooksCatalog.kt`
  - Убран импорт BookReaderMainActivity
- `app/src/main/java/su/sv/app/SvApp.kt`
  - Теперь наследуется от `Application` (не BookApplication)
  - Вызывает `BookReaderInitializer.init(this)` в onCreate
- `bookreader/src/main/AndroidManifest.xml`
  - Удалены декларации BookReaderMainActivity и SettingsActivity

### Этап 4: Settings

**Созданные файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderSettingsScreen.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderSettingsContent.kt`

**Удалённые файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/activities/SettingsActivity.kt`

**Добавленные строковые ресурсы (bookreader/src/main/res/values/strings.xml):**
```xml
<string name="menu_settings">Настройки</string>
<string name="theme_system">Системная</string>
<string name="theme_dark">Тёмная</string>
<string name="theme_light">Светлая</string>
```

---

## Текущая структура модуля bookreader

```
bookreader/src/main/java/com/github/axet/bookreader/
├── app/
│   ├── BookApplication.kt              # Делегирует к BookReaderInitializer
│   ├── BookReaderInitializer.kt        # ✅ Инициализация FBReader
│   ├── Storage.java                    # Работа с файлами книг
│   ├── TTFManager.kt                   # Шрифты
│   └── ...
├── activities/
│   └── ActivityExt.kt                  # Можно удалить
├── screens/
│   ├── ReaderScreen.kt                 # ✅ Modo Screen для чтения
│   ├── ReaderContent.kt                # ✅ Compose контент
│   ├── ReaderSettingsScreen.kt         # ✅ Modo Screen настроек
│   ├── ReaderSettingsContent.kt        # ✅ Compose настройки
│   ├── viewmodel/
│   │   ├── ReaderState.kt              # ✅ Sealed class состояний
│   │   ├── ReaderActions.kt            # ✅ Sealed class действий
│   │   └── ReaderViewModel.kt          # ✅ Hilt ViewModel
│   └── ui/
│       └── ReaderTopBar.kt             # ✅ Верхняя панель с меню
├── widgets/
│   ├── FBReaderView.java               # Главный виджет (не трогать)
│   └── ...                             # Другие виджеты
└── ...
```

---

## Навигация

**Открытие книги из books модуля:**
```kotlin
// BookDetailUi.kt
val uri = effect.book.fileUri
if (uri != null) {
    stackNavigation.forward(
        ReaderScreen(bookUri = uri)
    )
}
```

**Открытие настроек из читалки:**
```kotlin
// ReaderScreen.kt
ReaderContent(
    bookUri = bookUri,
    initialPosition = position,
    onNavigateToSettings = {
        stackNavigation.forward(ReaderSettingsScreen())
    },
)
```

---

## Что нужно сделать в Этапе 5

### 5.1 Очистка кода

**Файлы для удаления:**
- `bookreader/src/main/java/com/github/axet/bookreader/activities/ActivityExt.kt`
- `bookreader/src/main/res/layout/` - все XML layout (если не используются)
- `bookreader/src/main/res/xml/pref_general.xml` - старые настройки

**Проверить неиспользуемые импорты:**
- `BookApplication.from()` - deprecated, использовать `BookReaderInitializer.getTTFManager()`

### 5.2 Тестирование

**Функциональное тестирование:**
1. [ ] Открытие книги из списка книг
2. [ ] Открытие книги из деталей книги
3. [ ] Отображение контента книги
4. [ ] Перелистывание страниц (свайп, тап)
5. [ ] Прокрутка (непрерывный режим)
6. [ ] Переход к главе через TOC
7. [ ] Добавление закладки
8. [ ] Удаление закладки
9. [ ] Переход к закладке
10. [ ] Изменение размера шрифта
11. [ ] Изменение шрифта
12. [ ] Поиск по тексту
13. [ ] TTS (Text-to-Speech)
14. [ ] RTL режим
15. [ ] Сохранение позиции при выходе
16. [ ] Восстановление позиции при входе
17. [ ] Клавиши громкости для навигации
18. [ ] Полноэкранный режим
19. [ ] Тема (Light/Dark)
20. [ ] Настройки в ReaderSettingsScreen

**Навигация:**
- [ ] Назад из читалки → возврат в список книг
- [ ] Открытие читалки → корректный стек навигации
- [ ] Переход к настройкам и назад

**Устройства:**
- [ ] Телефон (портрет)
- [ ] Телефон (ландшафт)
- [ ] Планшет
- [ ] Android 8-10
- [ ] Android 11-13
- [ ] Android 14+

---

## Известные проблемы / TODO

1. **OnBookPagerManager** - пока передаётся `null` в `setActivity()`. Нужно инжектировать через Hilt.

2. **Диалоги не реализованы в Compose:**
   - TOC (Table of Contents) - используется `showToc` состояние
   - Bookmarks - используется `showBookmarks` состояние
   - Font Settings - используется `showFontSettings` состояние

3. **Настройки в ReaderSettingsContent:**
   - Папка синхронизации (требует SAF) - не реализована
   - TTS язык - не реализован
   - Блокировка экрана - не реализована

4. **Deprecated API:**
   - `PreferenceManager` - deprecated, но используется для совместимости

---

## Команда для компиляции

```bash
./gradlew :app:compileDebugKotlin --console=plain
```

---

## Полезные ссылки

- План миграции: `.claude/projects/-Users-i-u-voronin-StudioProjects-SV-APP/memory/bookreader-migration-plan.md`
- BookReaderInitializer: `bookreader/src/main/java/com/github/axet/bookreader/app/BookReaderInitializer.kt`
- ReaderScreen: `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderScreen.kt`
- ReaderViewModel: `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderViewModel.kt`
