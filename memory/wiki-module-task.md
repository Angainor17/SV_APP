---
name: wiki-module-task
description: Задача по реализации Wiki-модуля для приложения SV APP
metadata: 
  node_type: memory
  type: project
  originSessionId: f7026bbf-7256-4f62-8eca-c9484ece3941
---

# Wiki-модуль для SV APP

**Статус**: В разработке
**Источник**: https://svremya.su/ (MediaWiki)
**API**: Классический MediaWiki API (`/api.php`)

## Основные требования
- Поиск статей с отображением в БЛОКЕ ПРОСМОТРА
- Кликабельные гиперссылки в тексте статей
- Избранное (сохранение для оффлайн-доступа)
- История поиска

## Архитектура
- MVVM + Clean Architecture
- Hilt для DI
- Jetpack Compose + Material 3
- Room для локального хранилища

## Этапы реализации
1. Исследование API и документация
2. Data-слой (API и модели)
3. Локальное хранилище (Room)
4. Domain-слой (Use Cases)
5. Presentation-слой (ViewModel)
6. UI-компоненты (Compose)
7. Интеграция и тестирование

## Документы
- Формализованная задача: [[wiki_for_CLAUDE_fix]]
- Исходные требования: wiki_for_CLAUDE.md

**Why:** Проект требует отдельного Wiki-модуля для работы со словарём марксизма и диалектики с поддержкой оффлайн-режима.

**How to apply:** Следовать этапам из wiki_for_CLAUDE_fix.md. После каждого этапа запрашивать проверку у пользователя.
