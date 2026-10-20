#!/usr/bin/env bash
#
# Публикация сборки в RuStore через официальный REST API.
#
# Документация:
#   https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app
#   https://www.rustore.ru/help/work-with-rustore-api/api-authorization-token
#
# Что делает:
#   1. Собирает release (AAB или APK) через Gradle.
#   2. Авторизуется в API (JWE-токен из Key ID + приватного ключа).
#   3. Создаёт черновик версии (или переиспользует существующий).
#   4. Загружает AAB/APK.
#   5. (опц.) Отправляет на модерацию.
#   6. (опц.) Публикует (только для publishType=MANUAL и после модерации).
#
# Все секреты передаются через переменные окружения — НЕ хранятся в репозитории.
#
# Пример запуска:
#   RUSTORE_KEY_ID=123456 \
#   RUSTORE_PRIVATE_KEY='<base64-приватный-ключ>' \
#   RUSTORE_WHATS_NEW='Что нового в 0.3.6' \
#   ./scripts/publish-rustore.sh
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Конфигурация (env-переменные)
# ---------------------------------------------------------------------------
RUSTORE_KEY_ID="${RUSTORE_KEY_ID:-}"            # числовой ID ключа (консоль → API RuStore)
RUSTORE_PRIVATE_KEY="${RUSTORE_PRIVATE_KEY:-}"  # приватный ключ: base64 ИЛИ PEM-текст
RUSTORE_PACKAGE="${RUSTORE_PACKAGE:-su.sv.app}" # applicationId
RUSTORE_PUBLISH_TYPE="${RUSTORE_PUBLISH_TYPE:-MANUAL}" # MANUAL | INSTANTLY | DELAYED
RUSTORE_WHATS_NEW="${RUSTORE_WHATS_NEW:-}"      # "что нового" (changelog)
RUSTORE_DRAFT_JSON="${RUSTORE_DRAFT_JSON:-}"    # переопределить тело черновика целиком (JSON)
RUSTORE_VERSION_ID="${RUSTORE_VERSION_ID:-}"    # переиспользовать существующий черновик
RUSTORE_SUBMIT="${RUSTORE_SUBMIT:-1}"           # 1 = отправить на модерацию после загрузки
RUSTORE_PUBLISH="${RUSTORE_PUBLISH:-0}"         # 1 = опубликовать (MANUAL + уже прошла модерацию)
RUSTORE_ARTIFACT="${RUSTORE_ARTIFACT:-}"        # путь к AAB/APK (если не нужно пересобирать)
BUILD_VARIANT="${BUILD_VARIANT:-bundleRelease}" # bundleRelease (AAB) | assembleRelease (APK)

# Baseline Profile: перегенерировать перед сборкой, чтобы в релиз попал свежий профиль
# (ускоряет холодный старт). Требует подключённого эмулятора/устройства и подписи release
# через signingConfigs.release (RELEASE_STORE_* в env/local.properties). 0 = пропустить.
RUSTORE_BASELINE="${RUSTORE_BASELINE:-1}"
BASELINE_TASK=":app:generateReleaseBaselineProfile"

# Подпись (опционально). Release штатно подписывается через signingConfigs.release в
# app/build.gradle.kts (ключи из env/local.properties: RELEASE_STORE_FILE и т.д.).
# Если keystore передан ТОЛЬКО сюда (SIGN_*), артефакт подпишется здесь jarsigner'ом.
# RuStore требует подписанный AAB upload-ключом.
SIGN_STORE="${SIGN_STORE:-}"          # путь к .jks/.keystore
SIGN_ALIAS="${SIGN_ALIAS:-}"
SIGN_STORE_PASS="${SIGN_STORE_PASS:-}"
SIGN_KEY_PASS="${SIGN_KEY_PASS:-}"

API_BASE="https://public-api.rustore.ru/public/v1"
AUTH_URL="https://public-api.rustore.ru/public/auth"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

log()  { printf '\033[1;34m[rustore]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[rustore]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[rustore] ОШИБКА:\033[0m %s\n' "$*" >&2; exit 1; }
req()  { command -v "$1" >/dev/null 2>&1 || fail "нужна утилита '$1'"; }

# ---------------------------------------------------------------------------
# Авторизация
# ---------------------------------------------------------------------------
now_iso_ms() {
  python3 -c 'import datetime; print(datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="milliseconds").replace("+00:00","Z"))'
}

b64decode() {
  # читает base64 из stdin, печатает байты (кроссплатформенно)
  if [ "$(uname -s)" = "Darwin" ]; then base64 -D; else base64 -d; fi
}

write_key_pem() {
  local key="$1"
  if printf '%s' "$key" | grep -q -- '-----BEGIN'; then
    printf '%s\n' "$key" > "$WORKDIR/key.pem"
  else
    printf '%s' "$key" | tr -d '[:space:]' | b64decode > "$WORKDIR/key.pem"
  fi
  [ -s "$WORKDIR/key.pem" ] || fail "не удалось распарсить приватный ключ"
}

get_token() {
  local ts sig resp jwe
  write_key_pem "$RUSTORE_PRIVATE_KEY"
  ts="$(now_iso_ms)"
  # signature = base64( RSA-SHA512( keyId + timestamp ) )
  sig="$(printf '%s' "$RUSTORE_KEY_ID$ts" \
    | openssl dgst -sha512 -sign "$WORKDIR/key.pem" \
    | base64 | tr -d '\n')"
  resp="$(curl -sS -X POST "$AUTH_URL" \
    -H 'Content-Type: application/json' \
    -d "$(python3 -c 'import json,sys; print(json.dumps({"keyId":sys.argv[1],"timestamp":sys.argv[2],"signature":sys.argv[3]}))' "$RUSTORE_KEY_ID" "$ts" "$sig")")"
  jwe="$(printf '%s' "$resp" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("body",{}).get("jwe",""))' 2>/dev/null || true)"
  [ -n "$jwe" ] || { log "ответ auth: $resp"; fail "не удалось получить токен авторизации"; }
  printf '%s' "$jwe"
}

# ---------------------------------------------------------------------------
# Утилиты API
# ---------------------------------------------------------------------------
check_ok() { # exit 0 если body.code == "OK"
  printf '%s' "$1" | python3 -c 'import sys,json
try:
    sys.exit(0 if json.load(sys.stdin).get("code") == "OK" else 1)
except Exception:
    sys.exit(1)'
}

# api <METHOD> <path> [curl-args...] → печатает body ответа, на ошибку — fail
api() {
  local method="$1" path="$2"; shift 2
  local resp
  resp="$(curl -sS -X "$method" "$API_BASE$path" -H "Public-Token: $TOKEN" "$@")"
  if ! check_ok "$resp"; then fail "API ${method} ${path}: $resp"; fi
  printf '%s' "$resp"
}

# api_soft — как api, но на ошибку НЕ падает (возвращает 1), для необязательных шагов
api_soft() {
  local method="$1" path="$2"; shift 2
  local resp
  resp="$(curl -sS -X "$method" "$API_BASE$path" -H "Public-Token: $TOKEN" "$@")" || return 1
  if ! check_ok "$resp"; then warn "не OK (${method} ${path}): $resp"; return 1; fi
  printf '%s' "$resp"
}

# ---------------------------------------------------------------------------
# Сборка и подпись
# ---------------------------------------------------------------------------
find_artifact() {
  local dir ext
  case "$BUILD_VARIANT" in
    *bundle*) dir="app/build/outputs/bundle/release"; ext="aab";;
    *)        dir="app/build/outputs/apk/release";   ext="apk";;
  esac
  ls -t "$dir"/*."$ext" 2>/dev/null | head -n1
}

sign_artifact() {
  local artifact="$1"
  if [ -n "$SIGN_STORE" ]; then
    req jarsigner
    log "Подпись артефакта: jarsigner (alias=$SIGN_ALIAS)"
    jarsigner -keystore "$SIGN_STORE" \
      -storepass "$SIGN_STORE_PASS" -keypass "$SIGN_KEY_PASS" \
      "$artifact" "$SIGN_ALIAS"
    jarsigner -verify "$artifact" >/dev/null || fail "проверка подписи не прошла"
  else
    warn "SIGN_STORE не задан — артефакт должен быть уже подписан (signingConfig в Gradle)."
  fi
}

# ---------------------------------------------------------------------------
# Baseline Profile
# ---------------------------------------------------------------------------
# Перегенерирует baseline-prof.txt до сборки, чтобы в AAB/APK попал свежий профиль.
# Пропускает себя (с предупреждением), если нет устройства/эмулятора или RUSTORE_BASELINE=0.
regenerate_baseline_profile() {
  if [ "$RUSTORE_BASELINE" != "1" ]; then
    log "Baseline Profile: пропущено (RUSTORE_BASELINE=0)."
    return 0
  fi
  if ! command -v adb >/dev/null 2>&1 || [ "$(adb get-state 2>/dev/null)" != "device" ]; then
    warn "adb/эмулятор недоступен — Baseline Profile НЕ перегенерирован (в релиз попадёт закоммиченный профиль)."
    return 0
  fi
  log "Генерация Baseline Profile: ./gradlew $BASELINE_TASK"
  ./gradlew "$BASELINE_TASK"
}

# ---------------------------------------------------------------------------
# Основной флоу
# ---------------------------------------------------------------------------
main() {
  req curl; req openssl; req python3
  [ -n "$RUSTORE_KEY_ID" ] || fail "задай RUSTORE_KEY_ID"
  [ -n "$RUSTORE_PRIVATE_KEY" ] || fail "задай RUSTORE_PRIVATE_KEY"

  local artifact version_id

  if [ -n "$RUSTORE_ARTIFACT" ]; then
    artifact="$RUSTORE_ARTIFACT"
    [ -f "$artifact" ] || fail "файл не найден: $artifact"
  else
    regenerate_baseline_profile
    log "Сборка: ./gradlew $BUILD_VARIANT"
    ./gradlew "$BUILD_VARIANT"
    artifact="$(find_artifact)"
    [ -n "$artifact" ] || fail "не найден артефакт для $BUILD_VARIANT"
  fi
  log "Артефакт: $artifact"
  sign_artifact "$artifact"

  log "Авторизация в RuStore API…"
  TOKEN="$(get_token)"

  if [ -n "$RUSTORE_VERSION_ID" ]; then
    version_id="$RUSTORE_VERSION_ID"
    log "Переиспользую черновик versionId=$version_id"
  else
    local body resp
    if [ -n "$RUSTORE_DRAFT_JSON" ]; then
      body="$RUSTORE_DRAFT_JSON"
    else
      body="$(python3 -c 'import json,sys
d={"publishType":sys.argv[1]}
w=sys.argv[2]
if w: d["whatsNew"]=w
print(json.dumps(d, ensure_ascii=False))' "$RUSTORE_PUBLISH_TYPE" "$RUSTORE_WHATS_NEW")"
    fi
    log "Создание черновика версии…"
    resp="$(api POST "/application/$RUSTORE_PACKAGE/version" \
      -H 'Content-Type: application/json' -d "$body")"
    version_id="$(printf '%s' "$resp" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("body") or (d.get("content") or {}).get("versionId"))')"
    [ -n "$version_id" ] || fail "не получен versionId из ответа: $resp"
    log "Черновик создан, versionId=$version_id"
  fi

  local upload_path
  case "$artifact" in
    *.aab) upload_path="/application/$RUSTORE_PACKAGE/version/$version_id/aab";;
    *)     upload_path="/application/$RUSTORE_PACKAGE/version/$version_id/apk";;
  esac
  log "Загрузка файла…"
  api POST "$upload_path" -F "file=@$artifact"

  if [ "$RUSTORE_SUBMIT" = "1" ]; then
    log "Отправка на модерацию…"
    # Эндпоинт «Отправка на модерацию черновика» — свериться с доками при первом запуске.
    api_soft POST "/application/$RUSTORE_PACKAGE/version/$version_id/commit" \
      || warn "Отправка на модерацию не удалась — сделай это вручную в консоли."
  fi

  if [ "$RUSTORE_PUBLISH" = "1" ]; then
    log "Публикация версии…"
    api_soft POST "/application/$RUSTORE_PACKAGE/version/$version_id/publish" \
      || warn "Публикация не удалась (возможно, версия ещё на модерации)."
  fi

  log "Готово. versionId=$version_id, артефакт=$artifact"
}

main "$@"
