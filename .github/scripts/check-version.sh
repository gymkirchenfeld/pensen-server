#!/usr/bin/env bash
#
# Stellt sicher, dass die Versionsangaben des Projekts übereinstimmen.
#
#   check-version.sh              prüft pom.xml gegen Version.java
#   check-version.sh 3.10.0       prüft zusätzlich gegen die erwartete Version (Release-Tag)
#
# Hintergrund: Die Version steht an zwei Stellen. Weichen sie voneinander ab,
# meldet sich ein jar im Betrieb unter einer anderen Version als der, aus der es
# gebaut wurde — das ist später kaum zu diagnostizieren.

set -euo pipefail

VERSION_JAVA="src/main/java/ch/kinet/pensen/server/Version.java"

pom=$(mvn -q --batch-mode --no-transfer-progress \
      help:evaluate -Dexpression=project.version -DforceStdout)
src=$(sed -n 's/.*VERSION *= *"\(.*\)".*/\1/p' "$VERSION_JAVA")

echo "pom.xml:      ${pom:-<leer>}"
echo "Version.java: ${src:-<leer>}"

if [ -z "$pom" ]; then
    echo "::error file=pom.xml::Version konnte nicht ermittelt werden"
    exit 1
fi

if [ -z "$src" ]; then
    echo "::error file=$VERSION_JAVA::Version konnte nicht ermittelt werden"
    exit 1
fi

failed=0

if [ "$pom" != "$src" ]; then
    echo "::error file=$VERSION_JAVA::Version.java ($src) weicht von pom.xml ($pom) ab"
    failed=1
fi

if [ $# -ge 1 ]; then
    expected="$1"
    echo "erwartet:     $expected"
    if [ "$pom" != "$expected" ]; then
        echo "::error file=pom.xml::pom.xml ($pom) weicht von der erwarteten Version ($expected) ab"
        failed=1
    fi

    if [ "$src" != "$expected" ]; then
        echo "::error file=$VERSION_JAVA::Version.java ($src) weicht von der erwarteten Version ($expected) ab"
        failed=1
    fi
fi

if [ "$failed" -ne 0 ]; then
    exit 1
fi

echo "Versionen stimmen überein."