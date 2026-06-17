# Этап 3: Завершён

## Выполненные задачи

### 1. Добавлены зависимости Room
- ✅ Добавлена версия Room 2.7.1 в libs.versions.toml
- ✅ Добавлены библиотеки room-runtime, room-ktx, room-compiler
- ✅ Добавлен bundle для Room
- ✅ Подключены зависимости в wiki/build.gradle.kts

### 2. Созданы Entity классы
```
wiki/src/main/java/su/sv/wiki/data/local/entity/
├── FavoriteEntity.kt    # Избранные статьи
└── HistoryEntity.kt     # История поиска
```

**Таблица favorites:**
| Поле | Тип | Описание |
|------|-----|----------|
| title | String (PK) | Название статьи |
| content | String | HTML-содержимое |
| links | String | JSON со списком ссылок |
| savedAt | Long | Timestamp сохранения |

**Таблица history:**
| Поле | Тип | Описание |
|------|-----|----------|
| id | Long (PK) | Auto-generated ID |
| title | String | Название статьи |
| searchedAt | Long | Timestamp поиска |

### 3. Созданы DAO
```
wiki/src/main/java/su/sv/wiki/data/local/dao/
├── FavoriteDao.kt    # Операции с избранным
└── HistoryDao.kt     # Операции с историей
```

**FavoriteDao методы:**
- `getAllFavorites()` - Flow<List<FavoriteEntity>>
- `getFavoriteByTitle(title)` - suspend
- `isFavorite(title)` - suspend Boolean
- `insertFavorite(favorite)` - suspend
- `deleteFavorite(favorite)` - suspend
- `deleteFavoriteByTitle(title)` - suspend

**HistoryDao методы:**
- `getAllHistory()` - Flow<List<HistoryEntity>>
- `getRecentHistory(limit)` - Flow<List<HistoryEntity>>
- `insertHistory(history)` - suspend
- `deleteHistoryById(id)` - suspend
- `clearHistory()` - suspend
- `deleteOldHistory(keepCount)` - suspend

### 4. Создана Database
```
wiki/src/main/java/su/sv/wiki/data/local/database/WikiDatabase.kt
```
- Database version: 1
- Entities: FavoriteEntity, HistoryEntity
- DAOs: FavoriteDao, HistoryDao

### 5. Настроен Hilt-модуль для Database
```
wiki/src/main/java/su/sv/wiki/di/WikiDatabaseModule.kt
```
- Предоставление WikiDatabase
- Предоставление FavoriteDao
- Предоставление HistoryDao

### 6. Обновлён WikiRepository
Добавлены методы для локальных операций:
- `getFavorites()` - Flow<List<WikiArticle>>
- `isFavorite(title)` - suspend Boolean
- `addToFavorites(article)` - suspend
- `removeFromFavorites(title)` - suspend
- `getHistory()` - Flow<List<String>>
- `addToHistory(title)` - suspend
- `clearHistory()` - suspend

### 7. Обновлён WikiRepositoryImpl
- Инъекция FavoriteDao, HistoryDao, Gson
- Реализация всех локальных методов
- Маппинг Entity ↔ Domain моделей
- Сериализация/десериализация ссылок в JSON

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
│   │   ├── WikiApi.kt
│   │   └── model/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── FavoriteDao.kt
│   │   │   └── HistoryDao.kt
│   │   ├── database/
│   │   │   └── WikiDatabase.kt
│   │   └── entity/
│   │       ├── FavoriteEntity.kt
│   │       └── HistoryEntity.kt
│   └── repository/
│       └── WikiRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   └── WikiArticle.kt
│   └── repository/
│       └── WikiRepository.kt
└── di/
    ├── WikiApiModule.kt
    └── WikiDatabaseModule.kt
```

---

## Запрос на проверку

✅ Этап 3 завершён. Сборка успешна.

**Готов к переходу на Этап 4: Domain-слой (Use Cases)**
