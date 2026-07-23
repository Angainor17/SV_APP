# Руководство миграции Java → Kotlin

> Создано: 2026-07-23
> Цель: Универсальная стратегия для миграции любых Java классов

---

## Максимально эффективная стратегия

### Для Claude (AI)

**Проблема:** Миграция вручную требует переписывания 1000+ строк кода → много токенов, много ошибок

**Решение:** Человеко-машинное партнёрство

```
┌─────────────────────────────────────────────────────────────┐
│  Этап 1: Человек                                             │
│  - Открыть файл в Android Studio                            │
│  - Code → Convert Java File to Kotlin File                  │
│  - Сохранить .kt файл                                       │
│  - Удалить старый .java файл                                │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Этап 2: Claude                                              │
│  - Добавить wrapper'ы для родительских методов              │
│  - Добавить @JvmStatic/@JvmField                            │
│  - Сделать open class для наследуемых классов               │
│  - Компилировать → найти ошибки                             │
│  - Исправить критичные ошибки                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Этап 3: Claude + Человек                                    │
│  - Компиляция успешна → коммит                               │
│  - Или: исправить remaining nullable проблемы               │
└─────────────────────────────────────────────────────────────┘
```

### Почему так эффективнее

| Подход | Токены | Ошибки | Время |
|--------|--------|--------|-------|
| Claude пишет весь файл | 50,000+ | 20-50 | 30+ мин |
| Android Studio конвертирует | 0 | 5-10 | 2 мин |
| **Партнёрство** | 5,000 | 0-5 | 5-10 мин |

---

## Универсальные правила для всех классов

### 1. Проверить наследование ПЕРЕД миграцией

```bash
# Найти наследников класса
grep -rn "extends MyClass\|: MyClass\|: OuterClass\.InnerClass" --include="*.java" --include="*.kt"
```

Если есть наследники → класс должен быть `open`:

```kotlin
open class MyClass {
    open fun method() {}  // если переопределяется
}
```

### 2. Статические методы → @JvmStatic

```kotlin
class MyClass {
    companion object {
        @JvmStatic
        fun staticMethod() { }
        
        @JvmStatic
        fun loadPosition(json: JSONArray?): ZLTextPosition? { }
    }
}
```

### 3. Публичные поля → @JvmField

```kotlin
class MyClass {
    @JvmField var url: Uri? = null
    @JvmField var name: String? = null
}
```

### 4. Wrapper'ы для родительских методов

Если класс наследуется от другой Java библиотеки:

```kotlin
class Storage(context: Context) : com.github.axet.androidlibrary.app.Storage(context) {
    companion object {
        // Wrapper'ы для статических методов родителя
        @JvmStatic
        fun getFile(uri: Uri): File = com.github.axet.androidlibrary.app.Storage.getFile(uri)
        
        @JvmStatic
        fun exists(context: Context, uri: Uri): Boolean = 
            com.github.axet.androidlibrary.app.Storage.exists(context, uri)
    }
}
```

### 5. Nullable стратегия

**Правило:** Сначала оставить nullable (как в Java), потом оптимизировать

```kotlin
// Этап 1: Как в Java
var text: String? = null
var start: ZLTextPosition? = null

// Этап 2: После анализа использования
// Если 90% использования non-null → сделать non-null
var text: String = ""  // дефолт
var start: ZLTextPosition? = null  // оставить nullable
```

---

## Чек-лист миграции

### Перед миграцией (Человек)

- [ ] Проверить наследование класса
- [ ] Найти все использования внутренних классов
- [ ] Записать список wrapper'ов для родительских методов

### Конвертация (Человек)

- [ ] Открыть файл в Android Studio
- [ ] Code → Convert Java File to Kotlin File
- [ ] Сохранить .kt файл
- [ ] Удалить .java файл

### После конвертации (Claude)

- [ ] Добавить `open` для наследуемых классов
- [ ] Добавить `@JvmStatic` для статических методов
- [ ] Добавить `@JvmField` для публичных полей
- [ ] Добавить wrapper'ы для родительских методов
- [ ] Запустить компиляцию
- [ ] Исправить ошибки

### Финализация

- [ ] `./gradlew :module:compileDebugKotlin`
- [ ] Проверить WARNINGS
- [ ] Закоммитить

---

## Типичные ошибки и решения

| Ошибка | Причина | Решение |
|--------|---------|---------|
| `This type is final` | Класс наследуется | `open class` |
| `Cannot access` | Неправильный import | Проверить package |
| `Unresolved reference` | Нет @JvmStatic | Добавить |
| `Nullable type mismatch` | Kotlin nullable vs Java platform type | `!!` или `?.` |
| `No accessor` | Поле private | `@JvmField` |

---

## Примеры для SV APP

### Storage.java (1411 строк)

```
Наследники: Bookmark (в TTSPopup)
Wrapper'ы: getFile, exists, getName, getExt
Модификаторы: open class Bookmark
```

### ScrollWidget.java (1767 строк)

```
Наследники: нет
Внутренние классы: ScrollAdapter, PageView, PageHolder
Модификаторы: @JvmStatic для статических методов
```

### FBReaderView.java (2307 строк)

```
Наследники: нет
Внутренние классы: Listener, ZLBookmark, ZLTTSMark
Модификаторы: open для внутренних классов-наследников
```

---

## Команды для анализа

```bash
# Найти все Java файлы
find . -name "*.java" -type f

# Найти наследников
grep -rn "extends ClassName" --include="*.java" --include="*.kt"

# Найти использования внутренних классов
grep -rn "OuterClass\.InnerClass" --include="*.java" --include="*.kt"

# Размер файла
wc -l path/to/File.java
```

---

## Итог

**Для максимальной эффективности:**

1. **Человек** конвертирует через Android Studio (2 мин)
2. **Claude** добавляет модификаторы и wrapper'ы (3 мин)
3. **Вместе** исправляют nullable проблемы (5 мин)

**Итого: 10 минут на файл вместо 30+ минут вручную**