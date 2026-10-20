# SV APP

## Настройка окружения

### Репозитории зависимостей

Проект поддерживает два источника зависимостей:

| Платформа                  | Источник                  |
|----------------------------|---------------------------|
| macOS (корпоративная сеть) | Nexus (`nexus.vkteam.ru`) |
| Windows / внешний доступ   | Google Maven              |

#### Настройка

В файле `local.properties` (не коммитится в VCS) добавьте:

```properties
# Для macOS с доступом к nexus
useNexus=true

# Для Windows или без доступа к nexus
useNexus=false
```

По умолчанию используется `useNexus=true`.

## Релизный процесс

Порядок выпуска версии в RuStore. Все секреты (keystore, ключ RuStore, tracer-токены)
живут **вне git** — в `local.properties`, `scripts/rustore.env` или GitHub Secrets.

### 1. Поднять версию

В `app/build.gradle.kts` обнови `versionCode` (увеличить) и `versionName`.

### 2. Перегенерировать Baseline Profile

**Что это.** Baseline Profile — список «горячих» классов и методов, которые ART
AOT-компилирует сразу при установке. Ускоряет холодный старт приложения.

**Зачем перегенерировать.** Профиль привязан к конкретной версии кода: если код изменился,
а профиль — нет, он «протухает» и перестаёт помогать. Поэтому перед релизом его обновляют.

```bash
# нужен подключённый эмулятор/устройство и подписанный release (см. п. 3)
./gradlew :app:generateReleaseBaselineProfile
```

Результат: `app/src/release/generated/baselineProfiles/baseline-prof.txt` — коммитится в git.
Профиль **не зависит от стора и формата** (APK/AAB): он вшивается в артефакт и применяется
на устройстве через `profileinstaller`.

### 3. Подписать release

Подпись настроена в `app/build.gradle.kts` (`signingConfigs.release`) и читает ключи из
env / gradle-свойств / `local.properties`:

```properties
RELEASE_STORE_FILE=release/sv.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=…
RELEASE_KEY_PASSWORD=…
```

`SV.jks` и пароли **не коммитятся** (`*.jks` в `.gitignore`). Если keystore не задан,
release собирается без подписи.

### 4. Опубликовать в RuStore

**Локально (основной путь).** Скрипт перегенерирует Baseline Profile (если есть эмулятор),
собирает AAB, создаёт черновик и загружает его:

```bash
source scripts/rustore.env && ./scripts/publish-rustore.sh
```

**CI.** Воркфлоу `.github/workflows/publish-rustore.yml` запускается по тегу `v*` или
вручную. ⚠️ Сборка резолвит `ru.ok.tracer` только из внутреннего Nexus (`nexus.vkteam.ru`),
которого нет на GitHub-раннерах, поэтому воркфлоу работает только на self-hosted раннере
внутри сети VK (и для Baseline Profile ему нужен эмулятор).

### Секреты: где что лежит

| Секрет | Где хранится | Для чего |
|---|---|---|
| `SV.jks` + пароли | локально / GitHub Secrets | подпись release |
| `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | `scripts/rustore.env` / GitHub Secrets | API RuStore |
| tracer-токены (XOR+Base64) | `local.properties` / GitHub Secrets | `ru.ok.tracer` |
