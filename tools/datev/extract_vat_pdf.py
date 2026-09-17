#!/usr/bin/env python3
"""Convert a DATEV Koinos VAT-register PDF into Luna2's analytic CSV format.

The source report uses a fixed-width Type 3 font.  Text-copying therefore
produces spaced-out glyphs, while the glyph coordinates remain reliable.
This reader deliberately uses those coordinates and validates every document
before emitting rows.  It never derives accounting values from PDF summaries.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from pathlib import Path

try:
    import pdfplumber
except ImportError as exc:  # pragma: no cover - operator-facing dependency check
    raise SystemExit("Dipendenza mancante: installare pdfplumber (pip install pdfplumber).") from exc


MONEY = Decimal("0.01")
DATE_RE = re.compile(r"^\d{2}/\d{2}/\d{4}$")


@dataclass
class Document:
    page: int
    register: str
    movement_date: str
    protocol: str
    document_date: str
    document_number: str
    counterparty: str
    total: Decimal
    lines: list[dict[str, str]] = field(default_factory=list)


def text(chars: list[dict], start: float, end: float) -> str:
    selected = sorted(
        (c for c in chars if start <= float(c["x0"]) < end and not str(c["text"]).isspace()),
        key=lambda c: float(c["x0"]),
    )
    result: list[str] = []
    previous_end: float | None = None
    for char in selected:
        x0 = float(char["x0"])
        if previous_end is not None and x0 - previous_end > 2.2:
            result.append(" ")
        result.append(str(char["text"]))
        previous_end = float(char["x1"])
    return re.sub(r"\s+", " ", "".join(result)).strip()


def compact(chars: list[dict], start: float, end: float) -> str:
    return "".join(
        str(c["text"])
        for c in sorted(chars, key=lambda c: float(c["x0"]))
        if start <= float(c["x0"]) < end and not str(c["text"]).isspace()
    ).strip()


def money(value: str, *, required: bool = False) -> Decimal:
    value = value.strip()
    if not value:
        if required:
            raise ValueError("importo obbligatorio assente")
        return Decimal("0.00")
    negative = value.endswith("-")
    if negative:
        value = value[:-1].strip().lstrip("-")
    normalized = value.replace(".", "").replace(",", ".")
    if negative:
        normalized = "-" + normalized
    try:
        return Decimal(normalized).quantize(MONEY, rounding=ROUND_HALF_UP)
    except InvalidOperation as exc:
        raise ValueError(f"importo DATEV non valido: {value}") from exc


def iso_date(value: str) -> str:
    if not DATE_RE.match(value):
        raise ValueError(f"data DATEV non valida: {value}")
    day, month, year = value.split("/")
    return f"{year}-{month}-{day}"


def line_groups(page) -> list[tuple[float, list[dict]]]:
    groups: list[tuple[float, list[dict]]] = []
    for char in sorted(page.chars, key=lambda c: (float(c["top"]), float(c["x0"]))):
        if not groups or abs(float(char["top"]) - groups[-1][0]) > 0.8:
            groups.append((float(char["top"]), [char]))
        else:
            groups[-1][1].append(char)
    return groups


def register_title(groups: list[tuple[float, list[dict]]]) -> str:
    for top, chars in groups:
        if top > 14:
            break
        candidate = text(chars, 0, 595)
        if candidate and "PERIODO" not in candidate.upper():
            return candidate
    raise ValueError("titolo del registro IVA non riconosciuto")


def mapping(title: str) -> tuple[str, str]:
    upper = title.upper()
    if "CORRISPETT" in upper:
        register = "CORRISPETTIVI"
    elif "ACQUIST" in upper:
        register = "PURCHASES"
    else:
        register = "SALES"
    if "AUTOFATT" in upper:
        operation = "SELF_INVOICE"
    elif "REVERSE" in upper:
        operation = "REVERSE_CHARGE"
    elif "EXTRA UE" in upper or "EXTRA-UE" in upper:
        operation = "EXTRA_EU"
    elif "INTRA" in upper or re.search(r"\bUE\b", upper):
        operation = "INTRA_EU"
    elif "MARGINE" in upper:
        operation = "MARGIN"
    else:
        operation = "DOMESTIC"
    return register, operation


def rate_percent(rate_code: str) -> Decimal:
    digits = re.sub(r"\D", "", rate_code)
    if digits and Decimal(digits) <= 100:
        return Decimal(digits).quantize(MONEY)
    return Decimal("0.00")


def clean_counterparty(value: str) -> str:
    value = re.sub(r"^[A-Z0-9]{5,12}\s+", "", value.strip())
    return value[:190]


def load_articles(path: Path | None) -> dict[str, dict[str, str]]:
    if path is None:
        return {}
    raw = path.read_bytes()
    decoded = raw.decode("utf-8-sig", errors="replace")
    if "�" in decoded:
        decoded = raw.decode("cp1252", errors="replace")
    result: dict[str, dict[str, str]] = {}
    for row in csv.reader(decoded.splitlines(), delimiter=";"):
        if not row or not re.fullmatch(r"[A-Za-z0-9]+", row[0].strip()):
            continue
        code = row[0].strip()
        description = row[1].strip() if len(row) > 1 else ""
        nature = row[9].strip() if len(row) > 9 else ""
        if description:
            result[code] = {"description": description, "nature": nature}
    return result


def parse(pdf_path: Path, article_file: Path | None = None) -> tuple[list[dict[str, str]], list[str]]:
    rows: list[dict[str, str]] = []
    warnings: list[str] = []
    documents: list[Document] = []
    article_labels: dict[tuple[str, str, str], str] = {}
    summary_totals: dict[tuple[str, str, str], list[Decimal]] = defaultdict(lambda: [Decimal("0"), Decimal("0")])
    seen_rate_codes: dict[str, set[str]] = defaultdict(set)
    articles = load_articles(article_file)

    with pdfplumber.open(pdf_path) as pdf:
        for page_number, page in enumerate(pdf.pages, 1):
            groups = line_groups(page)
            title = register_title(groups)
            in_summary = not any(DATE_RE.match(compact(chars, 0, 49)) for _, chars in groups)
            current: Document | None = None

            for _, chars in groups:
                whole = text(chars, 0, 595)
                compact_whole = re.sub(r"\s+", "", whole).upper()
                if compact_whole.startswith("RIEPILOGO"):
                    in_summary = True
                    current = None
                    continue

                if in_summary:
                    rate_field = compact(chars, 0, 94)
                    rate_match = re.match(r"(\d{3}|[A-Z]{2})", rate_field)
                    rate_code = rate_match.group(1) if rate_match else ""
                    article_code = compact(chars, 94, 117)
                    description = text(chars, 117, 296)
                    if rate_code in seen_rate_codes[title] and re.fullmatch(r"[A-Z0-9]{2,5}", article_code) and description:
                        article_labels[(title, rate_code, article_code)] = description
                        taxable_summary = compact(chars, 278, 355)
                        vat_summary = compact(chars, 355, 418)
                        if taxable_summary and vat_summary:
                            summary_totals[(title, rate_code, article_code)][0] += money(taxable_summary)
                            summary_totals[(title, rate_code, article_code)][1] += money(vat_summary)
                    continue

                movement = compact(chars, 0, 49)
                protocol = compact(chars, 49, 99)
                if DATE_RE.match(movement) and protocol:
                    current = Document(
                        page=page_number,
                        register=title,
                        movement_date=iso_date(movement),
                        protocol=protocol,
                        document_date=iso_date(compact(chars, 99, 149)),
                        document_number=compact(chars, 149, 198),
                        counterparty=clean_counterparty(text(chars, 198, 399)),
                        total=money(compact(chars, 399, 449), required=True),
                    )
                    documents.append(current)
                    continue

                if current is None:
                    continue
                taxable_raw = compact(chars, 404, 449)
                rate_code = compact(chars, 449, 471)
                article_code = compact(chars, 471, 507)
                vat_raw = compact(chars, 507, 570)
                non_deductible_raw = compact(chars, 570, 595)
                if taxable_raw and rate_code and article_code and vat_raw:
                    if article_code.endswith("-"):
                        article_code = article_code[:-1]
                        vat_raw = "-" + vat_raw.lstrip("-")
                    current.lines.append({
                        "taxable": str(money(taxable_raw, required=True)),
                        "rate_code": rate_code,
                        "article_code": article_code,
                        "vat": str(money(vat_raw, required=True)),
                        "non_deductible": str(money(non_deductible_raw)),
                    })
                    seen_rate_codes[title].add(rate_code)
                elif not current.counterparty:
                    continuation = clean_counterparty(text(chars, 198, 404))
                    if continuation:
                        current.counterparty = continuation

    for document in documents:
        if not document.lines:
            warnings.append(f"pagina {document.page}, protocollo {document.protocol}: nessuna riga IVA")
            continue
        taxable_total = sum((Decimal(line["taxable"]) for line in document.lines), Decimal("0"))
        vat_total = sum((Decimal(line["vat"]) for line in document.lines), Decimal("0"))
        if min(abs(document.total - taxable_total), abs(document.total - taxable_total - vat_total)) > Decimal("0.03"):
            warnings.append(
                f"pagina {document.page}, protocollo {document.protocol}: totale documento {document.total} "
                f"diverso da imponibile+IVA {taxable_total + vat_total} (possibili componenti fuori campo o ritenute)"
            )
        register_type, operation_type = mapping(document.register)
        register_code = "DK-" + hashlib.sha1(document.register.encode("utf-8")).hexdigest()[:12].upper()
        for sequence, line in enumerate(document.lines, 1):
            vat_amount = Decimal(line["vat"])
            non_deductible = min(vat_amount, Decimal(line["non_deductible"]))
            deductible = vat_amount - non_deductible if register_type == "PURCHASES" or operation_type in {"REVERSE_CHARGE", "SELF_INVOICE"} else Decimal("0")
            due = vat_amount if register_type != "PURCHASES" or operation_type in {"REVERSE_CHARGE", "SELF_INVOICE"} else Decimal("0")
            percent = Decimal("100") if vat_amount == 0 else (deductible / vat_amount * 100).quantize(Decimal("0.0001"))
            source_parts = [document.register, document.movement_date, document.protocol, document.document_date,
                            document.document_number, line["rate_code"], line["article_code"], str(sequence)]
            source_key = hashlib.sha256("|".join(source_parts).encode("utf-8")).hexdigest()
            article = articles.get(line["article_code"], {})
            description = article.get("description") or article_labels.get((document.register, line["rate_code"], line["article_code"]), "")
            nature = article.get("nature", "")
            rows.append({
                "source_key": source_key,
                "register_type": register_type,
                "register_code": register_code,
                "register_name": document.register,
                "operation_type": operation_type,
                "collectability": "IMMEDIATE",
                "movement_date": document.movement_date,
                "protocol_number": document.protocol,
                "document_reference_date": document.document_date,
                "document_reference": document.document_number,
                "counterparty_name": document.counterparty,
                "description": f"Storico DATEV · {document.register} · pagina {document.page}",
                "vat_code": line["article_code"],
                "vat_rate": str(rate_percent(line["rate_code"])),
                "vat_nature": nature,
                "vat_description": description or f"Articolo IVA DATEV {line['article_code']}",
                "vat_legal_reference": "",
                "taxable_amount": line["taxable"],
                "vat_amount": line["vat"],
                "vat_due_amount": str(due.quantize(MONEY)),
                "deductible_vat": str(deductible.quantize(MONEY)),
                "deductibility_percent": str(percent),
                "datev_rate_code": line["rate_code"],
                "source_page": str(document.page),
            })
    detail_totals: dict[tuple[str, str, str], list[Decimal]] = defaultdict(lambda: [Decimal("0"), Decimal("0")])
    for row in rows:
        key = (row["register_name"], row["datev_rate_code"], row["vat_code"])
        detail_totals[key][0] += Decimal(row["taxable_amount"])
        detail_totals[key][1] += Decimal(row["vat_amount"])
    for key in sorted(set(detail_totals) | set(summary_totals)):
        detail = detail_totals.get(key, [Decimal("0"), Decimal("0")])
        summary = summary_totals.get(key, [Decimal("0"), Decimal("0")])
        if abs(detail[0] - summary[0]) > MONEY or abs(detail[1] - summary[1]) > MONEY:
            warnings.append(
                f"quadratura {key[0]} aliquota {key[1]} articolo {key[2]}: "
                f"dettaglio {detail[0]}/{detail[1]}, riepilogo {summary[0]}/{summary[1]}"
            )
    return rows, warnings


def main() -> int:
    parser = argparse.ArgumentParser(description="Estrae i registri IVA analitici DATEV Koinos in CSV Luna2.")
    parser.add_argument("pdf", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--articles", type=Path, help="Stampa Articoli IVA DATEV in CSV, usata per descrizione e natura fiscale.")
    parser.add_argument("--strict", action="store_true", help="Non scrive il CSV se sono presenti avvisi di quadratura.")
    args = parser.parse_args()
    if not args.pdf.is_file():
        parser.error(f"PDF non trovato: {args.pdf}")
    if args.articles is not None and not args.articles.is_file():
        parser.error(f"Stampa articoli IVA non trovata: {args.articles}")
    rows, warnings = parse(args.pdf, args.articles)
    if not rows:
        raise SystemExit("Nessun movimento IVA analitico riconosciuto.")
    for warning in warnings:
        print("AVVISO:", warning, file=sys.stderr)
    if args.strict and warnings:
        raise SystemExit(f"Estrazione interrotta: {len(warnings)} avvisi da verificare.")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    totals: dict[tuple[str, str], list[Decimal | int]] = defaultdict(lambda: [0, Decimal("0"), Decimal("0")])
    for row in rows:
        item = totals[(row["register_name"], row["movement_date"][:4])]
        item[0] += 1
        item[1] += Decimal(row["taxable_amount"])
        item[2] += Decimal(row["vat_amount"])
    print(f"Creato {args.output}: {len(rows)} righe IVA da {len(documents := set((r['register_name'], r['protocol_number'], r['movement_date']) for r in rows))} documenti.")
    for (register, year), (count, taxable, vat) in sorted(totals.items()):
        print(f"{year} | {register} | righe {count} | imponibile {taxable:.2f} | IVA {vat:.2f}")
    print(f"Avvisi: {len(warnings)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
