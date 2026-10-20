#!/usr/bin/env bash
#
# Публикация на сайт svremya.org (MediaWiki 1.43).
#
# Обновляет вики-страницу «Приложение СВ» (https://svremya.org/Приложение_СВ):
# ссылку на новый APK и номер версии. Дополнительно может залить собранный APK
# на сервер по FTP/FTPS/SFTP в каталог /lib/.
#
# Как это работает под капотом (MediaWiki API):
#   login token → action=login → csrf token → action=edit.
# Страница не защищена (protection: []), поэтому достаточно обычного аккаунта.
#
# ВАЖНО про APK: MediaWiki разрешает загрузку только png/gif/jpg/jpeg/webp,
# поэтому .apk через вики не заливается — файл кладётся на сервер по FTP/SSH
# в /lib/ (как текущий SV_app_0.1.7.apk). Порядок: сначала файл, потом ссылка.
#
# Секреты — только через env, в репозиторий не попадают.
#
# Пример:
#   WIKI_USER=... WIKI_PASS=... ./scripts/publish-site.sh
#
#   # + залить APK на сервер:
#   WIKI_USER=... WIKI_PASS=... \
#     APK_PATH=app/build/outputs/apk/release/app-release.apk \
#     SITE_FTP_HOST=svremya.org SITE_FTP_USER=... SITE_FTP_PASS=... \
#     ./scripts/publish-site.sh
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Конфигурация (env)
# ---------------------------------------------------------------------------
WIKI_API="${WIKI_API:-https://svremya.org/api.php}"
WIKI_PAGE="${WIKI_PAGE:-Приложение СВ}"
WIKI_USER="${WIKI_USER:-}"
WIKI_PASS="${WIKI_PASS:-}"

APP_VERSION="${APP_VERSION:-}"   # если пусто — читаем versionName из app/build.gradle.kts
APK_PATH="${APK_PATH:-}"         # путь к APK для заливки на сервер (опц.)

SITE_FTP_SCHEME="${SITE_FTP_SCHEME:-ftp}"   # ftp | ftps | sftp
SITE_FTP_HOST="${SITE_FTP_HOST:-}"
SITE_FTP_USER="${SITE_FTP_USER:-}"
SITE_FTP_PASS="${SITE_FTP_PASS:-}"
SITE_LIB_DIR="${SITE_LIB_DIR:-lib}"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT
COOKIES="$WORKDIR/cookies.txt"
UA="SV-APP-release-bot/1.0"

log()  { printf '\033[1;34m[site]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[site]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[site] ОШИБКА:\033[0m %s\n' "$*" >&2; exit 1; }
req()  { command -v "$1" >/dev/null 2>&1 || fail "нужна утилита '$1'"; }

# jget <json> <dotted.path> — значение по пути (списки адресуются индексом).
jget() {
  python3 -c 'import sys,json
d=json.loads(sys.argv[1])
for k in sys.argv[2].split("."):
    d = d[int(k)] if isinstance(d, list) else d[k]
sys.stdout.write(d if isinstance(d, str) else json.dumps(d, ensure_ascii=False))' "$1" "$2"
}

# ---------------------------------------------------------------------------
resolve_version() {
  if [ -z "$APP_VERSION" ]; then
    APP_VERSION="$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' app/build.gradle.kts | head -n1)"
    [ -n "$APP_VERSION" ] || fail "не удалось прочитать versionName из app/build.gradle.kts — задай APP_VERSION"
  fi
  APK_NAME="SV_app_${APP_VERSION}.apk"
  log "Версия: $APP_VERSION → файл на сайте /$SITE_LIB_DIR/$APK_NAME"
}

# ---------------------------------------------------------------------------
# MediaWiki: логин (login token → action=login)
# ---------------------------------------------------------------------------
wiki_login() {
  local login_token login_resp result

  login_token="$(jget "$(curl -sS -c "$COOKIES" -A "$UA" \
    "$WIKI_API?action=query&meta=tokens&type=login&format=json")" "query.tokens.logintoken")"

  login_resp="$(curl -sS -b "$COOKIES" -c "$COOKIES" -A "$UA" -X POST "$WIKI_API" \
    --data-urlencode "action=login" \
    --data-urlencode "lgname=$WIKI_USER" \
    --data-urlencode "lgpassword=$WIKI_PASS" \
    --data-urlencode "lgtoken=$login_token" \
    --data-urlencode "format=json")"

  result="$(jget "$login_resp" "login.result" 2>/dev/null || true)"
  if [ "$result" != "Success" ]; then
    warn "ответ логина: $login_resp"
    fail "не удалось войти в MediaWiki (login.result=$result). Проверь WIKI_USER/WIKI_PASS."
  fi
  log "Вошли в MediaWiki как $WIKI_USER."
}

# ---------------------------------------------------------------------------
# MediaWiki: обновить страницу (csrf → edit)
# ---------------------------------------------------------------------------
wiki_edit() {
  local csrf cur new_text edit_resp result

  csrf="$(jget "$(curl -sS -b "$COOKIES" -c "$COOKIES" -A "$UA" \
    "$WIKI_API?action=query&meta=tokens&type=csrf&format=json")" "query.tokens.csrftoken")"

  cur="$(jget "$(curl -sS -G -b "$COOKIES" -A "$UA" "$WIKI_API" \
    --data-urlencode "action=query" \
    --data-urlencode "prop=revisions" \
    --data-urlencode "rvprop=content" \
    --data-urlencode "rvslots=main" \
    --data-urlencode "titles=$WIKI_PAGE" \
    --data-urlencode "format=json" \
    --data-urlencode "formatversion=2")" "query.pages.0.revisions.0.slots.main.content")"

  # Заменяем версию в ссылке на APK: SV_app_<старая>.apk → SV_app_<новая>.apk.
  new_text="$(printf '%s' "$cur" | sed -E "s#SV_app_[0-9.]+\.apk#${APK_NAME}#g")"

  if [ "$new_text" = "$cur" ]; then
    log "Страница уже ссылается на $APK_NAME — правка не нужна."
    return 0
  fi

  edit_resp="$(curl -sS -b "$COOKIES" -c "$COOKIES" -A "$UA" -X POST "$WIKI_API" \
    --data-urlencode "action=edit" \
    --data-urlencode "title=$WIKI_PAGE" \
    --data-urlencode "text=$new_text" \
    --data-urlencode "summary=Обновление версии приложения до $APP_VERSION" \
    --data-urlencode "token=$csrf" \
    --data-urlencode "assert=user" \
    --data-urlencode "format=json")"

  result="$(jget "$edit_resp" "edit.result" 2>/dev/null || true)"
  if [ "$result" != "Success" ]; then
    warn "ответ edit: $edit_resp"
    fail "правка страницы «$WIKI_PAGE» не удалась (edit.result=$result)."
  fi
  log "Страница «$WIKI_PAGE» обновлена → ссылка на $APK_NAME."
}

# ---------------------------------------------------------------------------
# Заливка APK на сервер (FTP/FTPS/SFTP)
# ---------------------------------------------------------------------------
upload_apk() {
  [ -n "$APK_PATH" ] || return 0
  [ -f "$APK_PATH" ] || fail "APK не найден: $APK_PATH"

  if [ -n "$SITE_FTP_HOST" ] && [ -n "$SITE_FTP_USER" ]; then
    local url="$SITE_FTP_SCHEME://$SITE_FTP_HOST/$SITE_LIB_DIR/$APK_NAME"
    log "Заливка APK: $APK_PATH → $url"
    curl -T "$APK_PATH" "$url" --user "$SITE_FTP_USER:$SITE_FTP_PASS" \
      || fail "заливка APK по $SITE_FTP_SCHEME не удалась"
  else
    warn "APK НЕ залит (нет SITE_FTP_HOST/SITE_FTP_USER) — положи файл вручную: /$SITE_LIB_DIR/$APK_NAME"
  fi
}

# ---------------------------------------------------------------------------
main() {
  req curl; req python3; req sed
  [ -n "$WIKI_USER" ] || fail "задай WIKI_USER"
  [ -n "$WIKI_PASS" ] || fail "задай WIKI_PASS"

  resolve_version
  wiki_login
  upload_apk   # сначала файл
  wiki_edit    # потом ссылка на него

  log "Готово: версия $APP_VERSION опубликована на https://svremya.org/Приложение_СВ"
}

main "$@"
