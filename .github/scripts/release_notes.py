#!/usr/bin/env python3
"""Write the notes for a release from the commits since the previous one.

Usage: release_notes.py > notes.md

The commits are the ones since the newest tag, which is the previous release:
this runs before the new tag exists. Their subjects follow Conventional Commits,
so `feat:` goes under 新功能, `fix:` under 修正, and anything else a user would
notice under 其他. The release commits the workflow itself makes, and changes to
the build or CI, are left out.

The notes are only a starting point; edit them on GitHub afterwards if a
subject alone does not say enough.
"""

import re
import subprocess
import sys

SECTIONS = [
    ("feat", "新功能"),
    ("fix", "修正"),
    ("other", "其他"),
]

# Types that change nothing a user of the bundle can see.
SKIPPED = {"chore", "ci", "build", "test", "style"}

SUBJECT = re.compile(r"^(?P<type>[a-z]+)(?:\([^)]*\))?(?P<breaking>!)?:\s*(?P<text>.+)$")

FOOTER = "在 Morphe Manager 加入這個 patch 來源，或下載下面的 `.mpp` 手動匯入。"


def git(*args):
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True, encoding="utf-8"
    ).stdout.strip()


def previous_tag():
    try:
        return git("describe", "--tags", "--abbrev=0", "HEAD")
    except subprocess.CalledProcessError:
        return None  # The first release: every commit is new.


def subjects(since):
    revision = f"{since}..HEAD" if since else "HEAD"
    log = git("log", "--no-merges", "--reverse", "--format=%s", revision)
    return [line for line in log.splitlines() if line.strip()]


def render(lines):
    grouped = {key: [] for key, _ in SECTIONS}
    for line in lines:
        match = SUBJECT.match(line)
        if not match:
            grouped["other"].append(line)
            continue
        kind = match["type"]
        if kind in SKIPPED:
            continue
        text = match["text"]
        if match["breaking"]:
            text = "**不相容：** " + text
        grouped[kind if kind in grouped else "other"].append(text)

    out = []
    for key, title in SECTIONS:
        if grouped[key]:
            out.append(f"## {title}\n")
            out += [f"- {text}" for text in grouped[key]]
            out.append("")
    if not out:
        out = ["這一版沒有使用者看得到的變更。", ""]

    out.append(FOOTER)
    return "\n".join(out) + "\n"


def main():
    sys.stdout.write(render(subjects(previous_tag())))


if __name__ == "__main__":
    main()
