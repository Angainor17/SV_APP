# App Module

Главный модуль приложения.

## Обзор

Модуль `app` является точкой входа в приложение. Содержит Application класс и главную Activity.

## Основные компоненты

### SvApp
Application класс приложения:

```kotlin
class SvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация DI, библиотек и т.д.
    }
}
```

### MainActivity
Главная Activity приложения:

```kotlin
class MainActivity : AppCompatActivity() {
    // Содержит NavHost для навигации между экранами
    // И BottomNavigation
}
```

## Ответственности

- Инициализация DI (Hilt/Dagger)
- Настройка темы и UI
- Навигация между модулями
- Обработка deeplinks

## Структура файлов

```
app/src/main/java/su/sv/app/
├── SvApp.kt          # Application класс
└── MainActivity.kt   # Главная Activity
```

## Зависимости

Модуль зависит от всех feature-модулей:
- `main` — навигация
- `news` — лента новостей
- `books` — каталог книг
- `wiki` — wiki-страница
- `info` — информация
- `bookreader` — чтение книг

## AndroidManifest

В манифесте определены:
- `SvApp` как Application класс
- `MainActivity` как главная Activity с `LAUNCHER` intent-filter
- Необходимые разрешения (permissions)
