#!/usr/bin/env python3
"""
find_unused_code.py — находит потенциально мёртвые Kotlin/Java файлы во всех Gradle-модулях.

Что делает:
  1. Собирает исходники каждого модуля (src/main/**/*.kt|*.java).
  2. Извлекает из файлов объявления: top-level и вложенные типы (class/interface/object/enum/record),
     top-level функции (`fun name(`) и top-level свойства (`val`/`var`).
  3. Строит граф ссылок по простым именам (с учётом границ слова, `_` считается разделителем,
     поэтому JNI-символы вида `Java_..._Foo_bar` тоже считаются ссылкой на `Foo`).
  4. «Живость» распространяется от корневых точек входа:
       - AndroidManifest.xml (компоненты);
       - res/**/*.xml (кастомные View в layout и т.п.);
       - нативный код JNI (.c/.cpp/.cc/.cxx/.h/.hpp/.mm);
       - ProGuard-файлы (*.pro) — правила `-keep class ...`;
       - файлы с @Keep, @Module/@InstallIn/@EntryPoint/@HiltAndroidApp/@AndroidEntryPoint/@Inject (Hilt/Dagger),
         с `native`/`external fun` (JNI) и с top-level `fun main`.
  5. Мёртвый файл — тот, до которого живость не дотянулась.

ВАЖНО: скрипт НЕ удаляет код — он только печатает кандидатов. Каждый кандидат нужно проверять
вручную (возможны ложные срабатывания из-за рефлексии, кодогенерации, перегрузок и т.п.).

Использование:
  python3 scripts/find_unused_code.py                     # все модули
  python3 scripts/find_unused_code.py fbreader bookreader # выбранные модули
  python3 scripts/find_unused_code.py --orphans           # только «сироты» (без ссылок снаружи)
  python3 scripts/find_unused_code.py --list              # список файлов с числом объявлений

Зависимости: только стандартная библиотека Python 3.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# Модули из settings.gradle (include(":...")). Держим актуальным вручную.
DEFAULT_MODULES = [
    "app",
    "commonarchitecture",
    "commonui",
    "main",
    "books",
    "wiki",
    "news",
    "info",
    "models",
    "bookreader",
    "util",
    "fbreader",
    "managers",
    "api",
    "bugreport",
    "qa",
]

SOURCE_EXTS = {".kt", ".java"}
# Файлы, участвующие в графе ссылок (кроме исходников).
CORPUS_EXTS = {
    ".kt", ".java", ".xml", ".c", ".cpp", ".cc", ".cxx", ".h", ".hpp", ".mm", ".aidl", ".pro",
}

IDENT = r"[A-Za-z_$][A-Za-z0-9_$]*"

# --- Объявления -------------------------------------------------------------

# Тип: модификаторы + ключевое слово + имя. Работает и для Kotlin, и для Java.
TYPE_RE = re.compile(
    r"^\s*"
    r"(?:@\w+(?:\([^)]*\))?\s*)*"                              # аннотации
    r"(?:(?:public|private|protected|internal|abstract|open|final|sealed|data|inline|value|"
    r"annotation|enum|inner|static|strictfp|native|synchronized|transient|volatile|"
    r"default|non-sealed|fun)\s+)*"                              # модификаторы
    r"(?:class|interface|enum|object|record)\s+"                 # ключевое слово
    r"(" + IDENT + r")"                                          # имя
)
# Java-аннотация: @interface Name
AT_INTERFACE_RE = re.compile(r"^\s*@interface\s+(" + IDENT + r")")

# Top-level функция (строка без отступа, чтобы не цеплять методы внутри классов).
FUN_RE = re.compile(
    r"\bfun\s+"
    r"(?:<[^>\n]*>\s*)?"                                       # generic перед receiver
    r"(?:" + IDENT + r"(?:<[^>\n]*>\s*)?\?*\s*\.\s*)*"          # receiver(s), возможно generic/nullable
    r"(?:<[^>\n]*>\s*)?"                                       # generic перед именем
    r"(" + IDENT + r")\s*\("
)

# Top-level свойство (строка без отступа).
PROP_RE = re.compile(r"\b(?:val|var)\s+(" + IDENT + r")\b")


def extract_declarations(path: Path) -> set[str]:
    """Извлекает объявленные имена из исходного файла."""
    names: set[str] = set()
    try:
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError:
        return names
    for line in lines:
        stripped = line.lstrip()
        if not stripped or stripped.startswith(("//", "*", "/*", "import ", "package ")):
            continue
        # Только top-level объявления (колонка 0). Вложенные типы ловятся через
        # квалифицированное имя родителя (Foo.Bar всегда содержит "Foo"), а их простые
        # имена дают ложные ссылки (например, вложенный `object Action` совпадает со
        # словом "Action" по всему коду и не даёт найти мёртвый файл).
        if line[:1].isspace():
            continue
        m = TYPE_RE.match(line)
        if m:
            names.add(m.group(1))
            continue
        m = AT_INTERFACE_RE.match(line)
        if m:
            names.add(m.group(1))
            continue
        m = FUN_RE.search(line)
        if m:
            names.add(m.group(1))
            continue
        m = PROP_RE.search(line)
        if m:
            names.add(m.group(1))
    return names


# --- Сбор файлов ------------------------------------------------------------

def collect_module_files(module: str) -> tuple[list[Path], list[Path], list[Path]]:
    """Возвращает (исходники main, корпус для ссылок, proguard-файлы)."""
    mod_dir = ROOT / module
    sources: list[Path] = []
    corpus: list[Path] = []
    proguard: list[Path] = []

    for p in mod_dir.rglob("*"):
        s = str(p)
        if "/build/" in s or "/.gradle/" in s or "/generated/" in s:
            continue
        if not p.is_file():
            continue
        if p.suffix not in CORPUS_EXTS:
            continue
        corpus.append(p)
        if p.suffix in SOURCE_EXTS and "/src/main/" in s:
            sources.append(p)
        if p.suffix == ".pro":
            proguard.append(p)

    return sources, corpus, proguard


def is_seed(p: Path) -> bool:
    """Является ли файл корневой точкой входа (всегда «живой»)."""
    s = str(p)
    if p.name == "AndroidManifest.xml":
        return True
    if p.suffix == ".xml" and "/res/" in s:
        return True
    if p.suffix in {".c", ".cpp", ".cc", ".cxx", ".h", ".hpp", ".mm"}:
        return True
    if p.suffix == ".pro":
        return True
    if p.suffix in SOURCE_EXTS:
        try:
            text = p.read_text(encoding="utf-8", errors="replace")
        except OSError:
            return False
        markers = (
            "@Keep", "@Module", "@InstallIn", "@EntryPoint", "@HiltAndroidApp",
            "@AndroidEntryPoint", "@Inject",
        )
        if any(m in text for m in markers):
            return True
        if re.search(r"\bnative\s+[A-Za-z_$]", text):  # native-метод (JNI)
            return True
        if re.search(r"\bexternal\s+fun\b", text):     # Kotlin external fun (JNI)
            return True
        if re.search(r"\bfun\s+main\s*\(", text) or re.search(r"\bstatic\s+void\s+main\s*\(", text):
            return True
    return False


def word_pattern(name: str) -> re.Pattern:
    # Границы по [A-Za-z0-9]; `_` считается разделителем (для JNI-символов).
    return re.compile(r"(?<![A-Za-z0-9])" + re.escape(name) + r"(?![A-Za-z0-9])")


def main() -> int:
    ap = argparse.ArgumentParser(description="Поиск неиспользуемого кода в модулях.")
    ap.add_argument("modules", nargs="*", default=DEFAULT_MODULES,
                    help="модули для проверки (по умолчанию — все)")
    ap.add_argument("--orphans", action="store_true",
                    help="только типы без внешних ссылок (без reachability)")
    ap.add_argument("--list", action="store_true",
                    help="просто список исходников с числом объявлений")
    args = ap.parse_args()

    modules = args.modules
    if not modules:
        modules = DEFAULT_MODULES

    # Собираем все файлы и объявления.
    all_sources: list[Path] = []
    corpus: list[Path] = []
    seeds: set[Path] = set()
    file_names: dict[Path, set[str]] = {}
    name_to_files: dict[str, set[Path]] = {}

    for mod in modules:
        sources, mod_corpus, proguard = collect_module_files(mod)
        all_sources.extend(sources)
        corpus.extend(mod_corpus)
        for p in mod_corpus:
            if is_seed(p):
                seeds.add(p)
        for p in sources:
            names = extract_declarations(p)
            file_names[p] = names
            for n in names:
                name_to_files.setdefault(n, set()).add(p)

    if args.list:
        for p in sorted(all_sources):
            print(f"{len(file_names.get(p, set())):3d}  {p.relative_to(ROOT)}")
        return 0

    # Читаем корпус в память (для быстрых проверок вхождения).
    corpus_texts: list[tuple[Path, str]] = []
    for p in corpus:
        try:
            corpus_texts.append((p, p.read_text(encoding="utf-8", errors="replace")))
        except OSError:
            continue

    # Считаем внешние ссылки на каждое имя (исключая файлы, где имя объявлено).
    refs: dict[str, set[Path]] = {}
    all_names = sorted(name_to_files)
    for name in all_names:
        pat = word_pattern(name)
        declaring = name_to_files[name]
        ref_set: set[Path] = set()
        for p, text in corpus_texts:
            if p in declaring:
                continue
            if name in text and pat.search(text):
                ref_set.add(p)
        refs[name] = ref_set

    if args.orphans:
        for name in all_names:
            if not refs[name]:
                files = ", ".join(str(p.relative_to(ROOT)) for p in sorted(name_to_files[name]))
                print(f"{name}\t{files}")
        return 0

    # Reachability: живость от seeds через ссылки.
    alive_files: set[Path] = set(seeds) & set(all_sources)  # только исходники-сиды
    alive_files.update(seeds)                                # и не-исходники (манифесты/JNI/pro)
    # Для распространения нам нужны только исходники как цели.
    alive_sources: set[Path] = {p for p in all_sources if p in alive_files}

    # Граф: file -> set имён, на которые он ссылается (из корпуса).
    file_to_ref_names: dict[Path, set[str]] = {}
    for p, text in corpus_texts:
        found: set[str] = set()
        for name in all_names:
            if name in text:
                if word_pattern(name).search(text):
                    found.add(name)
        file_to_ref_names[p] = found

    queue: list[Path] = list(alive_files)
    while queue:
        f = queue.pop()
        for name in file_to_ref_names.get(f, set()):
            for target in name_to_files.get(name, set()):
                if target in all_sources and target not in alive_sources:
                    alive_sources.add(target)
                    queue.append(target)

    dead = sorted(set(all_sources) - alive_sources)

    print(f"Модулей: {len(modules)}; исходников: {len(all_sources)}; "
          f"объявленных имён: {len(all_names)}; мёртвых файлов: {len(dead)}\n")

    if not dead:
        print("Мёртвого кода не найдено.")
        return 0

    by_module: dict[str, list[Path]] = {}
    for p in dead:
        mod = p.relative_to(ROOT).parts[0]
        by_module.setdefault(mod, []).append(p)

    for mod in modules:
        mod_dead = by_module.get(mod)
        if not mod_dead:
            continue
        print(f"[{mod}]  {len(mod_dead)} файл(ов):")
        for p in sorted(mod_dead):
            names = sorted(file_names.get(p, set()))
            # показываем только «типовые» имена (не функции/свойства) кратко
            names_s = ", ".join(n for n in names if re.fullmatch(IDENT, n)) or "(нет типов)"
            print(f"  {p.relative_to(ROOT)}  ->  {names_s}")
        print()

    return 0


if __name__ == "__main__":
    sys.exit(main())
