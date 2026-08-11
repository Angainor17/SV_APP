# Debroid — отладка Android без GUI

## Что такое Debroid

**Debroid** — headless CLI-отладчик через JDWP. Позволяет AI-агенту отлаживать живое Android-приложение из терминала: ставить брейкпойнты, инспектить переменные, степпить, менять значения на лету.

## Быстрый старт

```bash
# Установка
curl -fsSL https://raw.githubusercontent.com/PatilShreyas/debroid/main/install.sh | bash

# Путь после установки
~/.local/bin/debroid

# Добавить в PATH:
export PATH="$HOME/.local/bin:$HOME/Library/Android/sdk/platform-tools:$PATH"
```

## Основные команды

### Запуск и подключение
```bash
# Запуск демона (фоном)
debroid daemon &

# Подключиться к уже запущенному приложению
debroid attach su.sv.app

# Запустить приложение с приостановкой (для отладки старта)
debroid launch su.sv.app
debroid resume <session_id>
```

### Брейкпойнты
```bash
# Установить брейкпойнт на строку файла (всегда с --package!)
debroid break <session_id> <FileName.kt> <line> --package su.sv.app

# Установить брейкпойнт на исключение
debroid catch-exception <session_id> --uncaught

# Посмотреть все активные точки
debroid points <session_id>
```

### Инспекция
```bash
# Получить состояние на текущей точке останова
debroid pause-state <session_id> <thread_id>

# Глубокий инспект объекта
debroid inspect <session_id> <object_id> --max-depth 3

# Выполнить выражение в VM
debroid eval <session_id> <thread_id> "viewModel.state.getValue()"

# Изменить переменную
debroid set-var <session_id> <thread_id> <varName> <newValue>
```

### Управление выполнением
```bash
# Поллинг событий (всегда с --with-stacktrace)
debroid poll <session_id> 0 --with-stacktrace

# Шаг
debroid step <session_id> <thread_id> STEP_OVER
debroid step <session_id> <thread_id> STEP_INTO
debroid step <session_id> <thread_id> RESUME_ALL

# Отключиться
debroid detach <session_id>

# Остановить демон
debroid stop
```

## Типичный сценарий дебага

1. `debroid daemon &`
2. `debroid attach su.sv.app` → получить `sessionId`
3. `debroid break <session_id> MainActivity.kt 50 --package su.sv.app`
4. `debroid poll <session_id> 0 --with-stacktrace`
5. Ждать событие `BREAKPOINT_HIT` → получить `threadId`
6. `debroid pause-state <session_id> <thread_id>` → locals, frames
7. `debroid inspect <session_id> <object_id>` → drill-down

## Важные правила

- **Всегда `--package`** при установке брейкпойнтов — ускоряет резолвинг класса
- **Всегда `--with-stacktrace`** при поллинге
- **ID сессии, брейкпойнтов, объектов** переиспользовать verbatim
- **Удаляй брейкпойнты**, когда они больше не нужны: `remove-break`
- **Compose**: брейкпойнты ставить на исполняемые строки внутри `@Composable`, а не на сигнатуру
- **Compose state**: `mutableStateOf` → переменная `$delegate` → `inspect` её `objectId`
- **Kotlin в eval**: свойства доступны через Java-геттеры: `obj.getName()`, не `obj.name`

## Особенности нашего проекта

| Параметр | Значение |
|----------|----------|
| App ID | `su.sv.app` |
| Главная Activity | `MainActivity.kt` |
| Пакет | `su.sv.app` |
| Compose | Да, везде |
| DI | Hilt |
| Навигация | Modo |

## Полезные брейкпойнты для расследования белого экрана

```bash
# Вход в MainActivity.setContent
debroid break <sid> MainActivity.kt 57 --package su.sv.app

# Вход в BottomNavigationBar
debroid break <sid> BottomNavigationUi.kt 118 --package su.sv.main

# Вход в SVAPPTheme
debroid break <sid> Theme.kt 45 --package su.sv.commonui

# Ловим все uncaught exceptions
debroid catch-exception <sid> --uncaught
```
