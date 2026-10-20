# Релизный процесс (полное руководство)

Пошаговая инструкция выпуска новой версии приложения. Написана так, чтобы её можно было
выполнить целиком вручную — без нейросети и без подглядывания в код.

## Каналы дистрибуции

| Канал | Что публикуем | Инструмент |
|---|---|---|
| **RuStore** | AAB | `scripts/publish-rustore.sh` (локально) или воркфлоу CI |
| **Сайт svremya.org** | APK + вики-страница «Приложение СВ» | `scripts/publish-site.sh` |

Оба канала независимы: можно выпускать в один и тот же релиз, или по отдельности.

---

## 0. Разовая настройка (только при первом релизе)

### 0.1. `local.properties` (не коммитится)

```properties
useNexus=true
# Подпись release
RELEASE_STORE_FILE=release/sv.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=…
RELEASE_KEY_PASSWORD=…
```

### 0.2. `scripts/rustore.env` (не коммитится)

Скопируй шаблон и заполни:

```bash
cp scripts/rustore.env.example scripts/rustore.env
```

Нужно заполнить:

- `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` — ключ API RuStore (консоль → «API RuStore»).
- `WIKI_USER`, `WIKI_PASS` — логин/пароль аккаунта на `svremya.org`, который может
  править страницу «Приложение СВ».
- (опц.) `SITE_FTP_*` — FTP-доступ к серверу сайта для заливки APK.

### 0.3. GitHub Secrets (для CI)

В `Settings → Secrets and variables → Actions`:
`SIGNING_KEYSTORE_B64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`,
`RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY`, `TRACER_*`, `WIKI_USER`, `WIKI_PASS`.

---

## 1. Поднять версию

В `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 16          // +1 к прошлому
    versionName = "0.3.6"     // новый номер
}
```

Зачем: RuStore и сайт различают версии по `versionName`, а `versionCode` должен монотонно
расти (иначе устройство не даст обновиться).

---

## 2. Перегенерировать Baseline Profile

Baseline Profile — список «горячих» классов/методов, которые ART AOT-компилирует при
установке. Ускоряет холодный старт. Профиль привязан к версии кода, поэтому перед релизом
его обновляют — иначе он «протухает» и не помогает.

```bash
# нужен подключённый эмулятор/устройство и подписанный release (п. 0.1)
./gradlew :app:generateReleaseBaselineProfile
```

Результат: `app/src/release/generated/baselineProfiles/baseline-prof.txt` — **коммитится**.
Профиль не зависит от стора и формата (APK/AAB) — вшивается в артефакт и применяется на
устройстве через `profileinstaller`.

---

## 3. Собрать подписанные артефакты

```bash
./gradlew assembleRelease   # APK — для сайта
./gradlew bundleRelease     # AAB — для RuStore
```

Если keystore задан (п. 0.1), оба артефакта подпишутся автоматически. Если нет — release
соберётся без подписи, и RuStore/сайт его не примут.

---

## 4. Опубликовать на сайт (svremya.org)

Страница: <https://svremya.org/Приложение_СВ>. Там две вещи: **файл APK** (на сервере, в
каталоге `/lib/`) и **ссылка на него в тексте вики**.

### Как это устроено

- **Текст страницы** правится через MediaWiki API (`login → edit`). Страница не защищена,
  достаточно обычного аккаунта.
- **Файл APK** через вики залить нельзя: MediaWiki разрешает только `png/gif/jpg/jpeg/webp`.
  Поэтому APK кладётся на сервер по FTP/SSH в `/lib/` (как текущий `SV_app_0.1.7.apk`).
- **Порядок важен:** сначала файл, потом ссылка — иначе страница будет ссылаться на
  несуществующий APK.

### Команда

```bash
source scripts/rustore.env

# только обновить текст страницы (файл уже лежит на сервере):
./scripts/publish-site.sh

# или сразу залить APK и обновить текст:
APK_PATH=app/build/outputs/apk/release/app-release.apk \
  SITE_FTP_HOST=svremya.org SITE_FTP_USER=… SITE_FTP_PASS=… \
  ./scripts/publish-site.sh
```

Скрипт сам читает `versionName` из `app/build.gradle.kts`, заменяет в тексте страницы
`SV_app_<старая>.apk` → `SV_app_<новая>.apk` и сохраняет правку.

---

## 5. Опубликовать в RuStore

```bash
source scripts/rustore.env && ./scripts/publish-rustore.sh
```

Что делает скрипт: (при эмуляторе) перегенерирует Baseline Profile → собирает AAB →
создаёт черновик в RuStore → загружает AAB → (опц.) отправляет на модерацию.

Флаги (env): `RUSTORE_PUBLISH_TYPE` (`MANUAL|INSTANTLY|DELAYED`), `RUSTORE_WHATS_NEW`,
`RUSTORE_SUBMIT` (1 = на модерацию), `RUSTORE_BASELINE` (1 = перегенерировать профиль).

---

## 6. CI (опционально)

Воркфлоу `.github/workflows/publish-rustore.yml` запускается по тегу `v*` или вручную и
делает: Baseline Profile → сборка AAB → RuStore → сайт (вики).

⚠️ **Ограничение:** `ru.ok.tracer` резолвится только из внутреннего Nexus
(`nexus.vkteam.ru`), которого нет на GitHub-раннерах. Поэтому воркфлоу работает только на
**self-hosted раннере внутри сети VK**, и ему нужен эмулятор (для Baseline Profile).

---

## Частые проблемы

| Симптом | Причина / решение |
|---|---|
| `login.result != Success` | Неверный логин/пароль. Либо вики требует бот-пароль: создай его в `Special:BotPasswords` и используй в `WIKI_USER`/`WIKI_PASS`. |
| `edit.result != Success` | Нет прав на страницу, или протух CSRF-токен (запусти скрипт заново — он берёт свежий токен). |
| FTP-заливка не проходит | Хост не принимает plain-FTP. Поставь `SITE_FTP_SCHEME=ftps` или `sftp`, либо залей файл через панель хостинга (cPanel/SSH) вручную. |
| Ссылка на сайте ведёт на старый APK | APK не залит, а текст обновился. Проверь, что файл `SV_app_<версия>.apk` реально лежит в `/lib/`. |
| Сборка падает на `ru.ok.tracer` | Нет доступа к Nexus. Собирай локально (`useNexus=true` в `local.properties`). |

---

## Секреты: сводная таблица

| Секрет | Где хранится | Для чего |
|---|---|---|
| `SV.jks` + пароли | локально / GitHub Secrets | подпись release |
| `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | `scripts/rustore.env` / GitHub Secrets | API RuStore |
| `WIKI_USER`, `WIKI_PASS` | `scripts/rustore.env` / GitHub Secrets | правка вики-страницы сайта |
| `SITE_FTP_*` | `scripts/rustore.env` | (опц.) заливка APK на сервер сайта |
| tracer-токены (XOR+Base64) | `local.properties` / GitHub Secrets | `ru.ok.tracer` |

Ничего из этого не должно попадать в git (`*.jks`, `*.env` — в `.gitignore`).
