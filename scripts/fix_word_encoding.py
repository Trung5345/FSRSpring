#!/usr/bin/env python3
"""Repair mojibake in vocabulary text fields.

The corrupted values look like UTF-8 text that was decoded as Windows-1252,
for example "khÃ¡ch sáº¡n" instead of "khách sạn" and
"/hoÊŠËˆtÉ›l/" instead of "/hoʊˈtɛl/".
"""

from __future__ import annotations

import argparse
import base64
import os
import subprocess
from datetime import datetime


MYSQL_HOST = os.getenv("MYSQL_HOST", "127.0.0.1")
MYSQL_PORT = os.getenv("MYSQL_HOST_PORT", "3307")
MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "fsrspring")
MYSQL_USER = os.getenv("MYSQL_USER", "fsr_user")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "fsr_pass")

TABLE_COLUMNS = {
    "words": [
        "word",
        "translation",
        "example",
        "pronunciation",
        "category",
        "part_of_speech",
        "synonyms",
        "antonyms",
        "origin",
    ],
    "topic": ["name", "slug", "description", "icon_emoji"],
}

CP1252_EXTRA = {
    "\u20ac": 0x80,
    "\u201a": 0x82,
    "\u0192": 0x83,
    "\u201e": 0x84,
    "\u2026": 0x85,
    "\u2020": 0x86,
    "\u2021": 0x87,
    "\u02c6": 0x88,
    "\u2030": 0x89,
    "\u0160": 0x8A,
    "\u2039": 0x8B,
    "\u0152": 0x8C,
    "\u017d": 0x8E,
    "\u2018": 0x91,
    "\u2019": 0x92,
    "\u201c": 0x93,
    "\u201d": 0x94,
    "\u2022": 0x95,
    "\u2013": 0x96,
    "\u2014": 0x97,
    "\u02dc": 0x98,
    "\u2122": 0x99,
    "\u0161": 0x9A,
    "\u203a": 0x9B,
    "\u0153": 0x9C,
    "\u017e": 0x9E,
    "\u0178": 0x9F,
}

BAD_MARKERS = (
    "Ã",
    "Â",
    "Ä",
    "Æ",
    "Ê",
    "Ë",
    "É",
    "Ð",
    "Ñ",
    "áº",
    "á»",
    "â€",
    "â€™",
    "â€œ",
    "â€",
    "â€“",
)


def mysql_base_cmd() -> list[str]:
    return [
        "mysql",
        "--protocol=TCP",
        "-h",
        MYSQL_HOST,
        "-P",
        MYSQL_PORT,
        "-u",
        MYSQL_USER,
        f"-p{MYSQL_PASSWORD}",
        "-D",
        MYSQL_DATABASE,
        "--batch",
        "--raw",
        "--skip-column-names",
    ]


def run_mysql(sql: str) -> str:
    proc = subprocess.run(
        mysql_base_cmd() + ["-e", sql],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return proc.stdout


def run_mysql_stdin(sql: str) -> None:
    subprocess.run(
        mysql_base_cmd(),
        check=True,
        text=True,
        input=sql,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )


def decode_cell(value: str) -> str | None:
    raw = bytearray()
    for ch in value:
        code = ord(ch)
        if code <= 0xFF:
            raw.append(code)
        elif ch in CP1252_EXTRA:
            raw.append(CP1252_EXTRA[ch])
        else:
            return None

    try:
        fixed = raw.decode("utf-8")
    except UnicodeDecodeError:
        return None

    return fixed if fixed != value else None


def bad_score(value: str) -> int:
    return sum(value.count(marker) for marker in BAD_MARKERS)


def should_fix(original: str, fixed: str | None) -> bool:
    if not fixed:
        return False
    return bad_score(fixed) < bad_score(original)


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def select_rows(table: str, columns: list[str]) -> list[tuple[int, dict[str, str | None]]]:
    encoded_columns = ", ".join(f"REPLACE(TO_BASE64(`{column}`), '\\n', '')" for column in columns)
    rows = []
    output = run_mysql(f"SET NAMES utf8mb4; SELECT id, {encoded_columns} FROM `{table}` ORDER BY id;")
    for line in output.splitlines():
        parts = line.split("\t")
        if not parts:
            continue
        row_id = int(parts[0])
        values: dict[str, str | None] = {}
        for column, encoded in zip(columns, parts[1:]):
            if encoded == "NULL":
                values[column] = None
            else:
                values[column] = base64.b64decode(encoded).decode("utf-8")
        rows.append((row_id, values))
    return rows


def build_updates() -> list[tuple[str, int, str, str, str]]:
    updates = []
    for table, columns in TABLE_COLUMNS.items():
        for row_id, values in select_rows(table, columns):
            for column, original in values.items():
                if not original:
                    continue
                fixed = decode_cell(original)
                if should_fix(original, fixed):
                    updates.append((table, row_id, column, original, fixed or ""))
    return updates


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="Apply updates. Default is dry-run.")
    args = parser.parse_args()

    updates = build_updates()
    print(f"Detected {len(updates)} corrupted cells.")
    for table, row_id, column, original, fixed in updates[:40]:
        print(f"{table}.{column}#{row_id}: {original} -> {fixed}")
    if len(updates) > 40:
        print(f"... and {len(updates) - 40} more")

    if not args.apply or not updates:
        return

    suffix = datetime.now().strftime("%Y%m%d_%H%M%S")
    statements = ["SET NAMES utf8mb4;"]
    for table in TABLE_COLUMNS:
        backup_table = f"{table}_encoding_backup_{suffix}"
        statements.append(f"CREATE TABLE `{backup_table}` AS SELECT * FROM `{table}`;")
    for table, row_id, column, _original, fixed in updates:
        statements.append(f"UPDATE `{table}` SET `{column}` = {sql_string(fixed)} WHERE id = {row_id};")
    run_mysql_stdin("\n".join(statements))
    print(f"Applied {len(updates)} updates. Backup suffix: {suffix}")


if __name__ == "__main__":
    main()
