"""
Fails the build if a Kotlin comment opens a nested block comment.

Kotlin nests block comments, unlike Java. A KDoc that mentions a glob such as
"content" followed by a slash-star therefore opens a second comment, and the
matching close then terminates only the inner one - silently swallowing the
declaration underneath. The symptom is a baffling KSP error saying a class
"could not be resolved", with no mention of a comment, so this is checked
directly rather than left to be rediscovered.

Run:  python android/tools/check_nested_comments.py
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def scan(path: Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    problems: list[str] = []
    index = depth = 0
    line = 1
    while index < len(text) - 1:
        pair = text[index:index + 2]
        if pair == "/*":
            if depth > 0:
                snippet = text[max(0, index - 40):index + 20].replace("\n", " | ")
                problems.append(f"{path.relative_to(ROOT)}:{line}: nested comment opened: ...{snippet}...")
            depth += 1
            index += 2
            continue
        if pair == "*/" and depth > 0:
            depth -= 1
            index += 2
            continue
        if text[index] == "\n":
            line += 1
        index += 1
    if depth != 0:
        problems.append(f"{path.relative_to(ROOT)}: unterminated block comment (depth {depth})")
    return problems


def main() -> int:
    problems = [
        problem
        for path in sorted((ROOT / "app" / "src").rglob("*.kt"))
        for problem in scan(path)
    ]
    for problem in problems:
        print(problem, file=sys.stderr)
    if problems:
        print(f"\n{len(problems)} nested block comment(s). Reword the comment, or close the inner one.", file=sys.stderr)
        return 1
    print("No nested block comments.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
