#!/usr/bin/env python3
"""Convert a DATEV Koinos analytical journal PDF to Luna2's balanced CSV format.

The converter is deliberately strict: it refuses output when an entry or the whole
book is not balanced. Original PDFs remain the legal/control evidence; the CSV is
the operational loading format.
"""

from __future__ import annotations

import argparse
import csv
import re
from dataclasses import dataclass, field
from decimal import Decimal
from pathlib import Path

from pypdf import PdfReader


MONEY = re.compile(r"(-?\d{1,3}(?:\.\d{3})*,\d{2})\s*$")
ACCOUNT = re.compile(r"^\s{8,20}(\d{3,12})\s+")
DATE = re.compile(r"^\d{2}/\d{2}/\d{4}$")


@dataclass
class Line:
    account: str
    account_name: str
    debit: Decimal
    credit: Decimal
    description: str


@dataclass
class Entry:
    entry_date: str
    competence_date: str
    document_date: str
    document_number: str
    cause_code: str
    description: str
    lines: list[Line] = field(default_factory=list)


def iso(value: str) -> str:
    if not value:
        return ""
    day, month, year = value.split("/")
    return f"{year}-{month}-{day}"


def amount(value: str) -> Decimal:
    return Decimal(value.replace(".", "").replace(",", "."))


def parse(path: Path) -> tuple[list[Entry], int]:
    entries: list[Entry] = []
    current: Entry | None = None
    orphan_headers = 0
    for page in PdfReader(str(path)).pages:
        for raw in (page.extract_text(extraction_mode="layout") or "").splitlines():
            if len(raw) >= 90 and DATE.fullmatch(raw[0:10]):
                if current is not None and not current.lines:
                    entries.pop()
                    orphan_headers += 1
                competence = raw[28:38].strip()
                document_date = raw[43:53].strip()
                cause = raw[86:91].strip()
                if not DATE.fullmatch(competence) or (document_date and not DATE.fullmatch(document_date)) or not cause:
                    raise ValueError(f"Testata non riconosciuta: {raw!r}")
                current = Entry(raw[0:10], competence, document_date, raw[68:86].strip(), cause, raw[92:].strip())
                entries.append(current)
                continue
            account_match = ACCOUNT.match(raw)
            money_match = MONEY.search(raw)
            if current is None or account_match is None or money_match is None:
                continue
            start = money_match.start(1)
            value = amount(money_match.group(1))
            debit, credit = (value, Decimal("0")) if start < 140 else (Decimal("0"), value)
            current.lines.append(Line(account_match.group(1), raw[43:83].strip(), debit, credit, raw[86:start].strip()))
    if current is not None and not current.lines:
        entries.pop()
        orphan_headers += 1
    return entries, orphan_headers


def validate(entries: list[Entry]) -> tuple[Decimal, Decimal]:
    debit = Decimal("0")
    credit = Decimal("0")
    for index, entry in enumerate(entries, 1):
        entry_debit = sum((line.debit for line in entry.lines), Decimal("0"))
        entry_credit = sum((line.credit for line in entry.lines), Decimal("0"))
        if not entry.lines or entry_debit != entry_credit:
            raise ValueError(
                f"Registrazione {index} non quadrata ({entry.entry_date}, {entry.cause_code}): "
                f"Dare {entry_debit}, Avere {entry_credit}"
            )
        debit += entry_debit
        credit += entry_credit
    if debit != credit:
        raise ValueError(f"Libro non quadrato: Dare {debit}, Avere {credit}")
    return debit, credit


def write(entries: list[Entry], destination: Path, year: str) -> int:
    destination.parent.mkdir(parents=True, exist_ok=True)
    fields = [
        "protocol", "entry_date", "competence_date", "document_date", "document_number",
        "cause_code", "entry_description", "account_code", "account_name", "debit", "credit", "line_description",
    ]
    rows = 0
    with destination.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter=";")
        writer.writeheader()
        for sequence, entry in enumerate(entries, 1):
            protocol = f"KOINOS-{year}-{sequence:06d}"
            for line in entry.lines:
                writer.writerow({
                    "protocol": protocol, "entry_date": iso(entry.entry_date),
                    "competence_date": iso(entry.competence_date), "document_date": iso(entry.document_date),
                    "document_number": entry.document_number, "cause_code": entry.cause_code,
                    "entry_description": entry.description, "account_code": line.account, "account_name": line.account_name,
                    "debit": f"{line.debit:.2f}", "credit": f"{line.credit:.2f}",
                    "line_description": line.description,
                })
                rows += 1
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--year", required=True, choices=("2025", "2026"))
    args = parser.parse_args()
    entries, orphan_headers = parse(args.source)
    debit, credit = validate(entries)
    rows = write(entries, args.destination, args.year)
    print(
        f"OK: {len(entries)} registrazioni, {rows} righe, "
        f"Dare={debit:.2f}, Avere={credit:.2f}, testate senza righe ignorate={orphan_headers}"
    )


if __name__ == "__main__":
    main()
