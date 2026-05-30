#!/usr/bin/env python3
"""
Add CRITICAL LOAD ORDER comment above Tailwind CDN script in all HTML files.
Run once; subsequent runs are idempotent.
"""
import os
import re
import glob

CDN_RE = re.compile(r'(<script src="https://cdn\.tailwindcss\.com[^"]*"></script>)')
GUARD_MARKER = "CRITICAL LOAD ORDER"
COMMENT = (
    "<!-- CRITICAL LOAD ORDER - DO NOT REORDER THESE TWO BLOCKS\n"
    "     1) CDN must load first -> defines the global `tailwind` object\n"
    "     2) tailwind.config block must come AFTER the CDN script\n"
    "     Swapping them silently breaks ALL custom colors/fonts/spacing.\n"
    "     Validated by: git pre-commit hook (.git/hooks/pre-commit) and\n"
    "                   scripts/validate-tailwind.js -->\n"
)

html_dir = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "static")
files = glob.glob(os.path.join(html_dir, "*.html"))

if not files:
    print("No HTML files found in", html_dir)
    exit(1)

for fpath in sorted(files):
    fname = os.path.basename(fpath)
    with open(fpath, encoding="utf-8") as f:
        content = f.read()

    if GUARD_MARKER in content:
        print(f"  already guarded: {fname}")
        continue

    new_content = CDN_RE.sub(COMMENT + r"\1", content, count=1)
    if new_content != content:
        with open(fpath, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"  guarded: {fname}")
    else:
        print(f"  WARNING - no CDN tag found: {fname}")
