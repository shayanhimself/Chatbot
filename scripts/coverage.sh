#!/usr/bin/env bash
# Measure line coverage per module and print it as one table, worst first.
#
# Not part of scripts/test.sh. No threshold is set, so this measures rather than gates. Kover
# already attaches koverVerify to `check`, which is where a threshold would gate once there is one.
#
# HTML for browsing a module's uncovered lines lands under <module>/build/reports/kover/html/.
set -euo pipefail

cd "$(dirname "$0")/.."

./gradlew koverXmlReport koverHtmlReport "$@"

# Builds the table: one row per module, worst coverage first, then a total.
#
# The XML is read rather than koverLog's console output: Gradle interleaves parallel task output,
# so attributing a printed percentage to a module means trusting the line ordering. A report file
# names its own module by its path.
python3 - <<'PY'
import pathlib
import xml.etree.ElementTree as ET

REPORT = "build/reports/kover/report.xml"
HEADER = ("module", "line", "covered", "missed")
PERCENT = 100

rows = []
for report in sorted(pathlib.Path().glob(f"**/{REPORT}")):
    module = ":" + str(report.parent.parent.parent.parent).replace("/", ":")
    for counter in ET.parse(report).getroot().findall("counter"):
        if counter.get("type") == "LINE":
            rows.append((module, int(counter.get("covered")), int(counter.get("missed"))))

if not rows:
    raise SystemExit("no coverage reports found; did koverXmlReport run?")

rows.sort(key=lambda row: row[1] / (row[1] + row[2]))
covered, missed = (sum(column) for column in zip(*(row[1:] for row in rows)))
rows.append(("TOTAL", covered, missed))

width = max(len(row[0]) for row in rows + [HEADER])
print()
print(f"{HEADER[0]:<{width}}  {HEADER[1]:>7}  {HEADER[2]:>7}  {HEADER[3]:>6}")
print("-" * (width + 26))
for module, covered, missed in rows:
    if module == "TOTAL":
        print("-" * (width + 26))
    print(f"{module:<{width}}  {covered / (covered + missed) * PERCENT:6.2f}%  {covered:>7}  {missed:>6}")
print()
PY
