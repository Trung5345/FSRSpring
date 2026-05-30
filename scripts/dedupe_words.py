#!/usr/bin/env python3
"""Deduplicate rows in the words table while preserving related references."""

from __future__ import annotations

import base64
import os
import subprocess
from collections import defaultdict
from datetime import datetime


MYSQL_HOST = os.getenv("MYSQL_HOST", "127.0.0.1")
MYSQL_PORT = os.getenv("MYSQL_HOST_PORT", "3307")
MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "fsrspring")
MYSQL_USER = os.getenv("MYSQL_USER", "fsr_user")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "fsr_pass")

TEXT_COLUMNS = [
    "word",
    "translation",
    "example",
    "pronunciation",
    "category",
    "part_of_speech",
    "synonyms",
    "antonyms",
    "origin",
    "audio_url",
    "image_url",
    "image_metadata_json",
    "enrichment_json",
]
OTHER_COLUMNS = ["difficulty", "cefr_level", "topic_id", "enrichment_status", "enriched_at"]
MERGE_COLUMNS = TEXT_COLUMNS + OTHER_COLUMNS
REFERENCE_TABLES = [
    "review_events",
    "user_progress",
    "vocabulary_set_word",
    "word_enrichment_jobs",
    "word_of_the_day",
]


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


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_value(value: str | None) -> str:
    if value is None or value == "NULL":
        return "NULL"
    return sql_string(value)


def select_words() -> list[dict[str, str | None]]:
    text_select = ", ".join(f"REPLACE(TO_BASE64(`{column}`), '\\n', '')" for column in TEXT_COLUMNS)
    other_select = ", ".join(f"CAST(`{column}` AS CHAR)" for column in OTHER_COLUMNS)
    output = run_mysql(f"SET NAMES utf8mb4; SELECT id, {text_select}, {other_select} FROM words ORDER BY id;")

    rows = []
    for line in output.splitlines():
        parts = line.split("\t")
        if not parts:
            continue
        row: dict[str, str | None] = {"id": parts[0]}
        text_values = parts[1 : 1 + len(TEXT_COLUMNS)]
        other_values = parts[1 + len(TEXT_COLUMNS) :]
        for column, encoded in zip(TEXT_COLUMNS, text_values):
            row[column] = None if encoded == "NULL" else base64.b64decode(encoded).decode("utf-8")
        for column, value in zip(OTHER_COLUMNS, other_values):
            row[column] = None if value == "NULL" else value
        rows.append(row)
    return rows


def quality(row: dict[str, str | None]) -> tuple[int, int]:
    important = [
        "translation",
        "pronunciation",
        "example",
        "image_url",
        "image_metadata_json",
        "enrichment_json",
        "synonyms",
        "antonyms",
        "origin",
        "topic_id",
        "cefr_level",
        "part_of_speech",
        "enriched_at",
    ]
    score = sum(1 for column in important if row.get(column))
    if row.get("enrichment_status") == "COMPLETED":
        score += 5
    return score, -int(row["id"] or "0")


def best_value(rows: list[dict[str, str | None]], column: str) -> str | None:
    for row in sorted(rows, key=quality, reverse=True):
        value = row.get(column)
        if value not in (None, ""):
            return value
    return None


def main() -> None:
    groups: dict[str, list[dict[str, str | None]]] = defaultdict(list)
    for row in select_words():
        word = (row.get("word") or "").casefold()
        groups[word].append(row)

    duplicate_groups = [rows for rows in groups.values() if len(rows) > 1]
    duplicate_rows = sum(len(rows) - 1 for rows in duplicate_groups)
    print(f"Duplicate groups: {len(duplicate_groups)}")
    print(f"Duplicate rows to remove: {duplicate_rows}")
    for rows in duplicate_groups[:20]:
        keep = sorted(rows, key=quality, reverse=True)[0]
        ids = ", ".join(row["id"] or "?" for row in rows)
        print(f"{keep.get('word')} -> keep {keep['id']} from [{ids}]")

    if duplicate_rows == 0:
        return

    suffix = datetime.now().strftime("%Y%m%d_%H%M%S")
    statements = [
        "SET NAMES utf8mb4;",
        f"CREATE TABLE `words_dedupe_backup_{suffix}` AS SELECT * FROM `words`;",
    ]
    for table in REFERENCE_TABLES:
        statements.append(f"CREATE TABLE `{table}_dedupe_backup_{suffix}` AS SELECT * FROM `{table}`;")

    delete_ids: list[str] = []
    for rows in duplicate_groups:
        keep = sorted(rows, key=quality, reverse=True)[0]
        keep_id = keep["id"]
        dup_ids = [row["id"] for row in rows if row["id"] != keep_id]
        delete_ids.extend(str(row_id) for row_id in dup_ids if row_id)

        assignments = []
        for column in MERGE_COLUMNS:
            value = best_value(rows, column)
            if value not in (None, "") and value != keep.get(column):
                assignments.append(f"`{column}` = {sql_value(value)}")
        if assignments:
            statements.append(f"UPDATE `words` SET {', '.join(assignments)} WHERE id = {keep_id};")

        id_list = ", ".join(str(row_id) for row_id in dup_ids if row_id)
        if id_list:
            for table in REFERENCE_TABLES:
                statements.append(f"UPDATE `{table}` SET word_id = {keep_id} WHERE word_id IN ({id_list});")

    if delete_ids:
        statements.append(f"DELETE FROM `words` WHERE id IN ({', '.join(delete_ids)});")
    statements.append("ALTER TABLE `words` ADD UNIQUE KEY `uk_words_word` (`word`);")

    run_mysql_stdin("\n".join(statements))
    print(f"Removed {duplicate_rows} duplicate rows. Backup suffix: {suffix}")


if __name__ == "__main__":
    main()
