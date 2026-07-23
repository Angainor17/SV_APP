# Руководство миграции Java → Kotlin

> Создано: 2026-07-23
> Цель: Минимизация багов при миграции legacy Java кода

---

## Ключевые принципы

### 1. Один файл = один коммит

Мигрировать файл целиком, сохраняя внутренние классы внутри.

```
❌ НЕ: Создавать отдельные .kt файлы для каждого внутреннего класса
✅ ДА: Конвертировать Storage.java → Storage.kt одним файлом
```

**Причина:** Java код использует `OuterClass.InnerClass`, отдельные файлы ломают API.

---

### 2. Сохранять package и импорты

```kotlin
// Java
package com.github.axet.bookreader.app;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

// Kotlin - тот же package, те же импорты
package com.github.axet.bookreader.app
import org.geometerplus.zlibrary.text.view.ZLTextPosition
```

---

### 3. Классы для наследования = `open`

Проверить наследование ПЕРЕД миграцией:

```bash
# Найти наследников
grep -rn "extends MyClass\|: MyClass\|: Storage\.Bookmark" --include="*.java" --include="*.kt"
```

```kotlin
// Если класс наследуется - обязательно open
open class Bookmark {
    open var color: Int = 0  // поле тоже open если переопределяется
}
```

**Пример из SV APP:**
- `Storage.Bookmark` наследуется в `TTSPopup.Fragment.Bookmark` → нужен `open class Bookmark`

---

### 4. @JvmStatic и @JvmField для Java-совместимости

```kotlin
class Storage {
    // Статические методы в companion object с @JvmStatic
    companion object {
        @JvmStatic
        fun loadPosition(json: JSONArray?): ZLTextPosition? { ... }

        @JvmStatic
        fun getAndroidId(context: Context): String { ... }
    }

    // Внутренние классы с @JvmField для public полей
    class Book {
        @JvmField var url: Uri? = null      // Java: book.url
        @JvmField var md5: String? = null   // Java: book.md5
        @JvmField var cover: File? = null
    }
}
```

---

### 5. Nullable типы = по анализу Java кода

```java
// Java - анализ кода
public ZLTextPosition position;  // может быть null? → проверить использование
public String title;             // может быть null? → проверить
```

```bash
# Проверить использование поля
grep -rn "\.position\s*=" --include="*.java"
grep -rn "\.position\s*!=" --include="*.java"
```

```kotlin
// Kotlin - только если действительно nullable
var position: ZLTextPosition? = null  // если было = null в Java
var title: String? = null              // если null возможен
```

---

## Пошаговый план миграции

### Шаг 1: Анализ файла

```bash
# 1. Размер файла
wc -l path/to/File.java

# 2. Количество внутренних классов
grep -c "public static class\|public class" path/to/File.java

# 3. Наследники классов
grep -rn "extends ClassName\|: ClassName" --include="*.java" --include="*.kt"

# 4. Использование внутренних классов
grep -rn "OuterClass\.InnerClass" --include="*.java" --include="*.kt"
```

### Шаг 2: Конвертация

**Вариант A: Android Studio (рекомендуется)**
1. Открыть файл в Android Studio
2. `Code → Convert Java File to Kotlin File`
3. Проверить результат

### Шаг 3: Добавить модификаторы

```kotlin
// Чек-лист:
// ☐ open class для наследуемых классов
// ☐ @JvmStatic для статических методов
// ☐ @JvmField для public полей, используемых из Java
// ☐ @Throws для методов с checked exceptions
```

### Шаг 4: Замена RuntimeException

```kotlin
// Java
throw new RuntimeException(e);

// Kotlin
throw IllegalStateException(e)
```

### Шаг 5: Проверка сборки

```bash
./gradlew :module:compileDebugKotlin :module:compileDebugJavaWithJavac
```

### Шаг 6: Коммит

```bash
git add path/to/File.kt
git commit -m "Convert File.java to Kotlin"
```

---

## Типичные ошибки и решения

| Ошибка | Причина | Решение |
|--------|---------|---------|
| `This type is final` | Класс наследуется, но не `open` | `open class MyClass` |
| `Cannot access class` | Неправильный import | Проверить package |
| `Unresolved reference` | Статический метод без `@JvmStatic` | Добавить `@JvmStatic` |
| `Nullable type mismatch` | Неправильный nullable тип | Проверить Java код |
| `'indexOf' hides member` | Метод переопределяет родительский | Добавить `override` |

---

## Особенности SV APP

### Тактика Nullable типов

При миграции Java → Kotlin, поля класса становятся nullable (`Type?`), что ломает код-потребитель.

**Стратегия:**

1. **Этап 1:** Мигрировать файл с nullable типами (как в Java)
2. **Этап 2:** После успешной сборки, постепенно убирать `?` где это безопасно:
   - Проверить все использования поля
   - Если 90% использования non-null → сделать поле non-null
   - Если много null-check → оставить nullable

**Пример для Bookmark:**

```kotlin
// Этап 1: nullable как в Java
@JvmField var text: String? = null
@JvmField var start: ZLTextPosition? = null
@JvmField var end: ZLTextPosition? = null

// Этап 2: после анализа использования
// text используется всегда → сделать non-null с дефолтом
@JvmField var text: String = ""
// start/end часто nullable → оставить nullable
@JvmField var start: ZLTextPosition? = null
```

**Правило:** Сначала добиваемся сборки с nullable, потом оптимизируем.

### Storage.java - внутренние классы

```
Storage.java (1411 строк)
├── Info          - используется в Plugin
├── Progress      - используется при загрузке
├── FBook         - книга в FBReader
├── Book          - книга в библиотеке
├── RecentInfo    - метаданные чтения
├── Bookmark      - закладка (наследуется в TTSPopup!) → OPEN
└── Bookmarks     - коллекция закладок
```

### Порядок миграции

1. Storage.java → Storage.kt
2. ScrollWidget.java → ScrollWidget.kt
3. FBReaderView.java → FBReaderView.kt