# SV APP

## Настройка окружения

### Репозитории зависимостей

Проект поддерживает два источника зависимостей:

| Платформа | Источник |
|-----------|----------|
| macOS (корпоративная сеть) | Nexus (`nexus.vkteam.ru`) |
| Windows / внешний доступ | Google Maven |

#### Настройка

В файле `local.properties` (не коммитится в VCS) добавьте:

```properties
# Для macOS с доступом к nexus
useNexus=true

# Для Windows или без доступа к nexus
useNexus=false
```

По умолчанию используется `useNexus=true`.
