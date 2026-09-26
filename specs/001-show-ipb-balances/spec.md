# Feature-Spezifikation: Anzeige der IPB-Saldi pro Schuljahr steuerbar

| | |
|---|---|
| **Feature-ID** | 001-show-ipb-balances |
| **Status** | Umgesetzt — offene Punkte in Abschnitt 9 entschieden |
| **Erstellt** | 2026-08-10 |
| **Betroffene Repositories** | `pensen-server`, `pensen-app` |
| **Vorgeschlagener Branch** | `001-show-ipb-balances` |

---

## 1. Zusammenfassung

Das Schuljahr erhält ein neues boolesches Feld `showIpbBalances`. Ist es `false`, werden für dieses
Schuljahr keine IPB-Saldi mehr angezeigt — weder im PDF-Pensenblatt noch im Frontend noch in den
CSV-Exporten. Die Berechnung der Saldi läuft unverändert weiter; es entfällt ausschliesslich die
Anzeige.

## 2. Motivation

Die beteiligten Schulen führen den Individuellen Pensenbuchhaltungs-Saldo (IPB) unterschiedlich.
Eine Instanz möchte die Saldi nicht mehr ausweisen, ohne die Datenbasis zu verlieren oder
Alt-Schuljahre umzuschreiben. Da sich die Praxis von Schuljahr zu Schuljahr ändern kann und
abgeschlossene Schuljahre reproduzierbar bleiben müssen, gehört die Einstellung auf die Ebene
Schuljahr und nicht in die Instanzkonfiguration (`client.features`).

## 3. Abgrenzung

### In Scope

- Neues Feld `showIpbBalances` auf dem Schuljahr, in der Datenbank persistiert und im
  Schuljahr-Dialog editierbar.
- Unterdrückung sämtlicher Anzeigen des **IPB-Saldos** (Anfangssaldo, Veränderung, Schlusssaldo,
  Saldo-Übersicht, IPB-Guthaben) in PDF, Frontend und CSV-Exporten.

### Out of Scope

- **IPB-Korrekturen.** Die Zeile „IPB-Korrektur \<Anstellungsart\>" im Lohnblock
  (`Payroll.java:84`) und die CSV-Spalten `S IPBKorr <code> L` (`PayrollCSVDownload.java:171`)
  sind eine Lohnkorrektur im Berechnungsmodus `lessons2` und haben mit dem Saldo nichts zu tun.
  Sie bleiben unverändert an `calculationMode == lessons2` gekoppelt.
- **Berechnung und Persistenz der Saldi.** `Workload.closingBalance`,
  `PensenData.recalculateBalance()`, die Fortschreibung Schlusssaldo → Anfangssaldo des Folgejahrs
  sowie die Jobs „IPB-Saldi aktualisieren" und „Schuljahr eröffnen" laufen unverändert.
- **Erfassung von IPB-Buchungen.** Der Menüpunkt „IPB-Buchungen" und die Erfassungsmaske bleiben
  erreichbar. Der Buchungsblock *auf dem Pensenblatt* wird hingegen mit ausgeblendet — siehe
  Punkt 9.1.
- Monotonie-Regeln über Schuljahre hinweg. Jedes Schuljahr wird unabhängig gesetzt.

## 4. Nutzungsszenarien

### Primäres Szenario

Eine Administratorin öffnet im Frontend den Schuljahr-Dialog, entfernt das Häkchen
„IPB-Saldi anzeigen" und speichert. Ab sofort erscheinen für dieses Schuljahr an keiner Stelle
mehr IPB-Saldi. Alle übrigen Schuljahre bleiben unverändert.

### Akzeptanzkriterien

| # | Gegeben | Wenn | Dann |
|---|---|---|---|
| AC-1 | Schuljahr mit `showIpbBalances = false` | PDF-Pensenblatt wird erzeugt | Der Block „IPB: Übersicht" fehlt vollständig; Kopfbereich, Kurse, Pool, Abschlussarbeiten, Zusammenfassung, Lohnblock und Unterschriftenblock sind unverändert |
| AC-1b | dito, Lehrperson **mit** IPB-Buchungen | PDF wird heruntergeladen oder per Mail versandt | Die Seite „Ein- und Ausbuchungen SJ …" fehlt. Zu prüfen für alle drei Wege: Einzeldownload, Sammeldownload, Mailversand |
| AC-2 | dito | Pensenblatt im Frontend wird geöffnet | Die Abschnitte „IPB: Übersicht" und „IPB: Ein- und Ausbuchungen" fehlen; die Server-Antwort enthält weder `balance` noch `postings` |
| AC-3 | dito | Anstellungsliste wird geöffnet | Die Spalten IPB-Anfangssaldo, IPB-Veränderung und IPB-Schlusssaldo fehlen inkl. Spaltenüberschriften; die Server-Antwort enthält die Felder `openingBalance`, `change` und `closingBalance` nicht |
| AC-4 | dito | Jahresüberblick wird geöffnet | Die Spalte „IPB-Guthaben" in der Abteilungstabelle fehlt inkl. Summenzeile |
| AC-4b | dito | Anstellungsliste wird geöffnet | Keine Zeile ist eingefärbt — insbesondere sind **nicht** alle Zeilen grün |
| AC-5 | dito | CSV-Export „Liste" (Anstellungen) wird erzeugt | Die drei IPB-Spalten fehlen in Kopfzeile und Daten; alle übrigen Spalten bleiben in Reihenfolge und Inhalt unverändert |
| AC-6 | dito | CSV-Export „Pensenmeldung" wird erzeugt | Die Spalte „IPB-Saldo Ende SJ" fehlt in Kopfzeile und Daten |
| AC-7 | dito, Berechnungsmodus `lessons2` | CSV-Export „Pensenmeldung" wird erzeugt | Die Spalten `S IPBKorr <code> L` sind weiterhin vorhanden und gefüllt |
| AC-8 | dito | Job „IPB-Saldi aktualisieren" läuft | Der Job berechnet und speichert die Saldi wie bisher; `employment.closing_balance` und der Anfangssaldo des Folgejahrs werden geschrieben |
| AC-9 | Schuljahr A mit `false`, Schuljahr B mit `true` | Lehrpersonen-Verlauf über beide Jahre wird geöffnet | Für B werden Saldi angezeigt, für A nicht — im selben Listenergebnis |
| AC-10 | Schuljahr mit `showIpbBalances = false` | Flag wird wieder auf `true` gesetzt | Alle Saldi erscheinen sofort wieder mit den fortlaufend berechneten Werten; kein Neuberechnungslauf nötig |
| AC-11 | Bestehende Datenbank | Migration wird eingespielt | Alle bestehenden Schuljahre haben `showIpbBalances = true`; das Verhalten ist unverändert |

### Randfälle

- **Gemischte Listen.** Anstellungslisten und der Lehrpersonen-Verlauf können Anstellungen aus
  mehreren Schuljahren enthalten. Die Unterdrückung muss pro Datensatz anhand des zugehörigen
  Schuljahrs entschieden werden, nicht global pro Anfrage (AC-9).
- **Fehlende Felder im Frontend.** Wo das Backend Felder weglässt, dürfen im Frontend keine
  `undefined`-Werte in Berechnungen einfliessen. Betroffen sind die Summenbildungen
  (`Overview.vue:36`, `DivisionTable.vue:69` addieren `closingBalance`) und die Zeilen-Einfärbung
  (`Employment.vue:201-217` wertet `item.change` aus). Beide müssen zusammen mit der jeweiligen
  Anzeige deaktiviert werden.
- **Stille Fehlklassifikation statt Absturz.** `rowClass` schwächt nicht ab, sondern kippt: mit
  `item.change === undefined` ergibt `Math.abs()` `NaN`, jeder Vergleich wird `false`, und die
  Methode fällt bis `'green lighten-2'` durch — **alle** Zeilen wären grün. Ein fehlendes Feld
  führt hier also nicht zu einer leeren, sondern zu einer falschen Anzeige (AC-4b).
- **Neu angelegtes Schuljahr.** Der Wert muss definiert sein, bevor der erste Workload berechnet
  wird (siehe offener Punkt 9.3).

## 5. Funktionale Anforderungen

**Datenmodell**

- **FR-001** — Das Schuljahr besitzt ein nicht-nullbares boolesches Attribut `showIpbBalances` mit
  Vorgabewert `true`.
- **FR-002** — Bestehende Schuljahre erhalten bei der Migration den Wert `true`, damit sich das
  Verhalten ohne aktive Umstellung nicht ändert.

**Bearbeitung**

- **FR-003** — Das Attribut ist im Schuljahr-Bearbeitungsdialog als Checkbox editierbar und
  unterliegt derselben Berechtigung wie die übrigen Schuljahr-Attribute (`isEditAllowed`).
- **FR-004** — Eine Änderung des Attributs löst **keine** Neuberechnung der Saldi aus.

**Unterdrückung im Backend**

- **FR-005** — Ist `showIpbBalances = false`, liefert die Anstellung im JSON die Felder
  `openingBalance`, `closingBalance` und `change` nicht aus.
- **FR-006** — Ist `showIpbBalances = false`, liefert der Workload im JSON das Feld `balance`
  nicht aus.
- **FR-007** — Ist `showIpbBalances = false`, enthält das PDF-Pensenblatt den Block
  „IPB: Übersicht" nicht. Dies gilt für alle Erzeugungswege (Einzeldownload, Sammeldownload,
  E-Mail-Versand).
- **FR-008** — Ist `showIpbBalances = false`, entfallen im CSV-Export der Anstellungen die drei
  Spalten IPB-Anfangssaldo, IPB-Veränderung und IPB-Schlusssaldo — in Kopfzeile **und** Daten,
  sodass die Spaltenzahl konsistent bleibt.
- **FR-009** — Ist `showIpbBalances = false`, entfällt im CSV-Export der Pensenmeldung die Spalte
  „IPB-Saldo Ende SJ" — in Kopfzeile und Daten.
- **FR-010** — Die Entscheidung wird je Datensatz anhand des Schuljahrs des jeweiligen Objekts
  getroffen, nicht anhand eines global ausgewählten Schuljahrs.
- **FR-017** — Ist `showIpbBalances = false`, liefert der Workload im JSON auch das Feld
  `postings` nicht aus (Entscheid 9.1). Die Erfassung von Buchungen bleibt davon unberührt.
- **FR-019** — Ist `showIpbBalances = false`, enthält das PDF auch die Seite
  „Ein- und Ausbuchungen SJ …" nicht. Diese Seite wird von einem **eigenen** Generator
  (`PostingsPDFGenerator`) erzeugt, nicht vom Pensenblatt-Generator, und hat drei eigene
  Aufrufstellen.

**Unterdrückung im Frontend**

- **FR-011** — Das Attribut wird im Schuljahr-JSON ausgeliefert, damit das Frontend
  Spaltenüberschriften und Beschriftungen unterdrücken kann.
- **FR-012** — Ist `showIpbBalances = false`, blendet die Anstellungsliste die drei IPB-Spalten
  aus.
- **FR-013** — Ist `showIpbBalances = false`, blendet der Jahresüberblick die Spalte
  „IPB-Guthaben" samt Summenbildung aus.
- **FR-018** — Ist `showIpbBalances = false`, färbt die Anstellungsliste ihre Zeilen nicht mehr
  nach der IPB-Veränderung ein. Alle Zeilen werden neutral dargestellt.
- **FR-014** — Für den Abschnitt „IPB: Übersicht" auf dem Pensenblatt ist keine eigene
  Frontend-Bedingung erforderlich: das Ausbleiben des Feldes `balance` genügt (FR-006). Diese
  Eigenschaft ist bei der Umsetzung zu erhalten.

**Unverändertes Verhalten**

- **FR-015** — Die Berechnung und Persistenz der Saldi ist von diesem Attribut unabhängig.
- **FR-016** — IPB-Korrekturen (Lohnblock und CSV) sind von diesem Attribut unabhängig.

## 6. Nicht-funktionale Anforderungen

- **NFR-001 — Backend zuerst.** Die Unterdrückung erfolgt so weit wie möglich serverseitig durch
  Weglassen der Felder im JSON. Im Frontend verbleiben nur Anpassungen, die serverseitig nicht
  möglich sind, weil sie statische Beschriftungen betreffen (Spaltenüberschriften) oder
  Berechnungen im Client auslösen (Summenzeilen).
- **NFR-002 — Keine Datenlecks.** Wo Felder unterdrückt werden, dürfen sie in keiner Antwort
  desselben Endpunkts über einen anderen Weg zurückkommen (z. B. verbose statt terse).
- **NFR-003 — Auslieferungsreihenfolge.** Server- und Frontend-Änderung müssen **gemeinsam**
  ausgeliefert werden. Ein Server-only-Rollout stürzt nicht ab, zeigt aber die Anstellungsliste
  vollständig grün eingefärbt (siehe Randfall „Stille Fehlklassifikation"). Ein reiner
  Frontend-Rollout ist unschädlich: `showIpbBalances` fehlt dann im JSON, und alle Prüfungen auf
  `!== false` liefern das bisherige Verhalten.
- **NFR-004 — Namenskonvention.** Datenbankspalte in `snake_case`, Property-Name in `PascalCase`,
  JSON-Schlüssel in `camelCase` — analog zu `smallGroupSurcharge` / `small_group_surcharge` /
  `SmallGroupSurcharge`.

## 7. Inventar der betroffenen Anzeigestellen

Vollständige Liste der Stellen, an denen heute IPB-**Saldi** erscheinen, mit dem jeweils
vorgesehenen Vorgehen.

| # | Stelle | Anzeige | Vorgehen |
|---|---|---|---|
| 1 | `WorkloadPDFGenerator.java:57`, `:325-370` | Block „IPB: Übersicht" im PDF | Backend: Block überspringen |
| 1b | `PostingsPDFGenerator.java:46`, aufgerufen aus `WorkloadDownload.java:87,94` und `WorkloadMail.java:134` | Ganze PDF-Seite „Ein- und Ausbuchungen SJ …" | Backend: in `create()` früh aussteigen (FR-019) |
| 2 | `Workload.java:148`, `:158-166` | Feld `balance` im Workload-JSON → `Balance.vue` | Backend: Feld weglassen |
| 2b | `Workload.java:145` | Feld `postings` im Workload-JSON → `Postings.vue` | Backend: Feld weglassen (Entscheid 9.1) |
| 3 | `Employment.java:174-175`, `:180` | `change`, `closingBalance`, `openingBalance` im JSON | Backend: Felder weglassen |
| 4 | `Employment.vue:123-140` | Drei Spaltenüberschriften | Frontend: Spalten filtern |
| 4b | `Employment.vue:201-217` | Zeilen-Einfärbung nach IPB-Veränderung | Frontend: Einfärbung abschalten (FR-018) |
| 5 | `DivisionTable.vue:60`, `:69`; `Overview.vue:36`, `:88` | Spalte „IPB-Guthaben" + Summe | Frontend: Spalte und Summenbildung filtern |
| 6 | `EmploymentCSVDownload.java:84-86`, `:108-110` | Drei CSV-Spalten | Backend: Spalten weglassen |
| 7 | `PayrollCSVDownload.java:85`, `:156` | CSV-Spalte „IPB-Saldo Ende SJ" | Backend: Spalte weglassen |

**Nicht betroffen, aber im Umfeld:**

| Stelle | Warum unverändert |
|---|---|
| `Payroll.java:84`, `PayrollCSVDownload.java:171` | IPB-Korrektur, nicht Saldo (Out of Scope) |
| `menu.js:55`, `Posting/*.vue`, `PostingResource.java` | Erfassung von Buchungen bleibt möglich |
| `CalculateBalances.java`, `InitializeSchoolYear.java:96` | Berechnung läuft weiter |
| `TeacherHistory.vue:27-29` | Bindet an `year.ipbEnd`; der Server liefert `closingBalance` (`Employment.java:38`). Die Anzeige ist bereits heute leer. Wird die Bindung je korrigiert, muss sie gemäss FR-005 mitgeführt werden |
| `SubjectCategoryTable.vue:29-31` | Slot `item.closingBalance` ohne passenden Header (`:60-72`); wird nie gerendert |
| `WorkloadPDFGenerator.java:56` | Der dortige `postingsBlock()` ist auskommentiert. Die Buchungsseite entsteht stattdessen in `PostingsPDFGenerator` — siehe Nummer 1b oben. Die Textzeile `:352` („siehe nächste Seite") bezieht sich auf jene Seite und ist konsistent, sobald beide gemeinsam aus- oder eingeblendet werden |

## 8. Schlüsselbegriffe

- **IPB-Saldo** — über Schuljahre fortgeschriebenes Guthaben in Prozent. Zusammensetzung:
  Anfangssaldo + Pensum + Ein-/Ausbuchungen − Auszahlung = Schlusssaldo (`Workload.java:67-70`).
  Gegenstand dieser Spezifikation.
- **IPB-Korrektur** — Korrektur der Lohnmeldung in Lektionen, nur im Berechnungsmodus `lessons2`.
  Trotz des ähnlichen Namens ein anderes Konzept und **nicht** Gegenstand dieser Spezifikation.
- **IPB-Buchung** (`Posting`) — manuell erfasste Ein- oder Ausbuchung, die in den Saldo einfliesst.

## 9. Entschiedene Punkte

> Alle drei Punkte wurden am 2026-08-10 gemäss Empfehlung entschieden.

### 9.1 Buchungsblock auf dem Pensenblatt — **mit ausblenden**

Der Abschnitt „IPB: Ein- und Ausbuchungen" (`Postings.vue:3`, gespeist aus `workload.postings`)
zeigt keine Saldi, sondern die Buchungen selbst. Nach dem Wortlaut der Anforderung bleibt er
sichtbar.

**Entschieden: mit ausblenden.** Die Buchungen sind ohne die Saldo-Übersicht kaum interpretierbar,
und die Zeile „Ein- und Ausbuchungen" der Übersicht verschwindet ohnehin. Technisch identisch
gelöst wie FR-006: Feld `postings` im Workload-JSON weglassen — `Workload.vue:27-30` prüft bereits
`workload.postings`, eine Frontend-Änderung ist nicht nötig. Im PDF ist der Block bereits
auskommentiert. Ergänzt als **FR-017**: Ist `showIpbBalances = false`, liefert der Workload im
JSON auch das Feld `postings` nicht aus. Die Erfassung von Buchungen über den Menüpunkt
„IPB-Buchungen" bleibt davon unberührt.

### 9.2 Schreibweise des JSON-Schlüssels — **`showIpbBalances`**

Angefordert ist `showIpbBalances`. Die bestehende Konvention im Projekt schreibt das Akronym
klein: `ipbCorrectionAllowed` (`PayrollType.java:29`), `ipbCorrectionLessons`
(`Payroll.java:308`).

**Entschieden: `showIpbBalances`** für Konsistenz mit der bestehenden Konvention. Auf den
Datenbank-Property-Namen hat das keinen Einfluss — dieser lautet `ShowIpbBalances` und bildet auf
die Spalte `show_ipb_balances` ab.

### 9.3 Vorgabewert bei neuen Schuljahren — **vom Vorjahr übernehmen**

Beim Anlegen eines Schuljahres (`SchoolYearResource.java:66-108`) ist der Wert festzulegen.

**Entschieden: vom Vorjahr übernehmen**, analog zum Kleingruppen-Zuschlag
(`SchoolYearResource.java:84-87`), mit Rückfall auf `true`, wenn kein Vorjahr existiert. Eine
Instanz, die die Saldi abgeschaltet hat, will sie im Folgejahr in aller Regel nicht wieder
eingeblendet bekommen. Die Vererbungsregel liegt ausschliesslich im Server; das Frontend sendet
das Feld beim Anlegen nicht mit.

## 10. Prüfliste

- [x] Offene Punkte 9.1 bis 9.3 entschieden
- [x] Alle sieben Anzeigestellen aus Abschnitt 7 abgedeckt
- [x] IPB-Korrekturen nachweislich unberührt (AC-7) — Code-Prüfung; Laufzeittest offen
- [ ] Migration eingespielt und Vorgabewert `true` geprüft (AC-11)
- [ ] Gemischte Schuljahre im selben Listenergebnis geprüft (AC-9)
- [ ] Wiedereinschalten ohne Neuberechnung geprüft (AC-10)
- [ ] PDF-Ausgabe geprüft (AC-1)
- [ ] Frontend-Ansichten geprüft (AC-3, AC-4) — `npm install` in `pensen-app` erforderlich

---

Technische Umsetzung siehe [plan.md](plan.md).