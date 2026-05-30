#!/usr/bin/env python3
"""
Fix corrupted CRITICAL LOAD ORDER comments in HTML files.
The corrupted comment starts with '<-  flutter-app/...' (malformed <!--)
and ends with '-->'. Replace with a clean, valid HTML comment.
"""
import re
import glob
import os

# Pattern: match the corrupted comment block
# It starts with '<-' (broken <!--) and ends with '-->'
CORRUPTED_RE = re.compile(
    r'  <-.*?-->\n',
    re.DOTALL
)

CLEAN_COMMENT = (
    "  <!-- CRITICAL LOAD ORDER: DO NOT REORDER THESE TWO BLOCKS.\n"
    "       Rule: CDN script must load FIRST (defines window.tailwind),\n"
    "       then tailwind.config block runs. Wrong order = all custom\n"
    "       colors/fonts/spacing silently broken. See .git/hooks/pre-commit. -->\n"
)

html_dir = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "static")
files = sorted(glob.glob(os.path.join(html_dir, "*.html")))

if not files:
    print("No HTML files found in", html_dir)
    exit(1)

for fpath in files:
    fname = os.path.basename(fpath)
    with open(fpath, encoding="utf-8") as f:
        content = f.read()

    if "<-  flutter" not in content:
        print(f"  OK (no corruption): {fname}")
        continue

    new_content = CORRUPTED_RE.sub(CLEAN_COMMENT, content, count=1)
    if new_content == content:
        print(f"  WARNING - could not fix: {fname}")
        continue

    with open(fpath, "w", encoding="utf-8") as f:
        f.write(new_content)
    print(f"  Fixed: {fname}")
