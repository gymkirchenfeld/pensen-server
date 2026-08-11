# Umsetzungsplan: 001-show-ipb-balances

Technische Umsetzung zu [spec.md](spec.md). Ausgangsstand: `45f5f69` (`pensen-server`) bzw.
`main` von `pensen-app`. Die Entscheide zu den Punkten 9.1 bis 9.3 der Spezifikation sind hier
eingearbeitet.

**Status: umgesetzt.** Zeilenangaben beziehen sich auf den Stand *vor* der Änderung.

---

## 1. Namensschema

| Ebene | Bezeichner |
|---|---|
| Datenbankspalte | `pensen.school_year.show_ipb_balances` |
| Property-Name (Reflection) | `ShowIpbBalances` |
| Java-Konstante DB | `SchoolYear.DB_SHOW_IPB_BALANCES = "ShowIpbBalances"` |
| Java-Konstante JSON | `SchoolYear.JSON_SHOW_IPB_BALANCES = "showIpbBalances"` |
| Java-Zugriff | `isShowIpbBalances()` / `setShowIpbBalances(boolean)` |

Die Abbildung Property-Name → Spaltenname erfolgt durch `ch.kinet.datalib` per Reflection, analog
zu `DB_SMALL_GROUP_SURCHARGE = "SmallGroupSurcharge"` → `small_group_surcharge`. Der Setter ist
zwingend erforderlich, damit die Spalte beim Laden befüllt wird (Vorbild: `setArchived`).

## 2. Datenbank

### 2.1 Migration

`db/migration/v3_10_0__add_show_ipb_balances_to_school_year.sql`:

```sql
ALTER TABLE pensen.school_year ADD COLUMN show_ipb_balances boolean NOT NULL DEFAULT true;
```

Konvention und Stil folgen `db/migration/v3_9_0__add_small_group_surcharge_to_school_year.sql`.
Der Vorgabewert `true` erfüllt FR-002 ohne separates UPDATE.

### 2.2 Schema

`db/create.sql`, Tabelle `pensen.school_year`: Spalte
`show_ipb_balances boolean not null default true` neben `small_group_surcharge` ergänzt.

## 3. Server

### 3.1 `data/SchoolYear.java`

- Konstanten `DB_SHOW_IPB_BALANCES` und `JSON_SHOW_IPB_BALANCES` in den bestehenden
  alphabetischen Gruppen.
- Feld `private boolean showIpbBalances;`, Getter `isShowIpbBalances()`, Setter
  `setShowIpbBalances(boolean)`.
- Ausgeliefert in **`toJsonTerse()`**, nicht in `toJsonVerbose()`. Grund: `Workload`
  (`Workload.java:142`) und `Employment` (`Employment.java:183`) betten das Schuljahr terse ein.
  Die terse Variante hält das Flag überall verfügbar und kostet ein Boolean.

### 3.2 `data/PensenData.java` — `createSchoolYear` (`:282-297`)

Parameter `boolean showIpbBalances` ergänzt und in die `PropertyMap` aufgenommen.

### 3.3 `server/SchoolYearResource.java`

- **`create()`**: Der Wert wird mit
  `data.getBoolean(JSON_SHOW_IPB_BALANCES, lastSchoolYear == null || lastSchoolYear.isShowIpbBalances())`
  gelesen — fehlt das Feld im Request, erbt das neue Schuljahr vom zuletzt angelegten; existiert
  keines, gilt `true`. Die bestehende lokale Variable `previousForSurcharge` (`:84-85`) dient nun
  beiden Vorgabewerten und heisst darum `lastSchoolYear`.
- **`update()`**: analog zu `archived` gelesen und mit `Util.equal` verglichen; bei Änderung
  Setter und `changed.add(SchoolYear.DB_SHOW_IPB_BALANCES)`.
  **`recalculate` bleibt unberührt** (FR-004) — mit Kommentar im Code festgehalten.
  Als Vorgabewert dient `object.isShowIpbBalances()`, nicht `false` wie bei `archived`: ein
  Request ohne das Feld darf den Wert nicht kippen.

### 3.4 `data/Employment.java` — `toJsonTerse()`

Die drei Saldo-Felder stehen jetzt in einem Block hinter `if (schoolYear.isShowIpbBalances())`.
Da `Employment` sein Schuljahr selbst kennt, erfüllt diese Stelle FR-010 automatisch — auch in
gemischten Listen wie dem Lehrpersonen-Verlauf (`PensenData.java:695-698`).

`Employment` überschreibt `toJsonVerbose()` nicht; die verbose Antwort in
`EmploymentResource.java:77` fällt auf `toJsonTerse()` zurück. NFR-002 ist damit erfüllt.

### 3.5 `calculation/Workload.java` — `toJsonTerse()`

`balance()` **und** `postings` stehen hinter `if (schoolYear.isShowIpbBalances())` (FR-006,
FR-017). `balance()`, `closingBalance` und `getClosingBalance()` selbst bleiben unverändert —
letzteres wird weiterhin von `recalculateBalance` benötigt.

### 3.6 PDF — zwei Generatoren, nicht einer

**`job/WorkloadPDFGenerator.java` — `create()`:** `balanceBlock()` wird nur noch aufgerufen, wenn
`workload.getSchoolYear().isShowIpbBalances()`.

**`job/PostingsPDFGenerator.java` — `create()`:** früher Ausstieg mit derselben Bedingung, direkt
neben der bestehenden Prüfung `workload.postings().isEmpty()`.

Die Buchungsseite „Ein- und Ausbuchungen SJ …" stammt **nicht** aus dem Pensenblatt-Generator —
dessen `postingsBlock()` ist auskommentiert (`:56`) — sondern aus dieser zweiten Klasse mit
eigenem `createPDF`-Einstieg. Sie wird an drei Stellen aufgerufen: `WorkloadDownload.java:87`
(Einzeldownload), `:94` (Sammeldownload) und `WorkloadMail.java:134` (Mailversand). Der Riegel
sitzt darum in `create()` und nicht an den Aufrufstellen — eine Stelle statt drei.

> Diese Klasse fehlte in der ersten Inventur, weil sie weder „IPB" noch „Saldo" enthält und über
> die Stichwortsuche nicht auffindbar war. Gefunden im Code-Review.

### 3.7 `job/EmploymentCSVDownload.java`

- `createHeaders()` lieferte einen festen `Stream.of(...)`; jetzt eine `List<String>` mit
  bedingtem Anhängen der drei Spalten — Vorbild ist `PayrollCSVDownload.createHeaders()`.
  Neue Importe: `java.util.ArrayList`, `java.util.List`.
- `run()`: die drei `csv.append(...)` hinter demselben Flag.

**Beide Jobs lesen das Flag genau einmal in `run()` und reichen es als Parameter an
`createHeaders()` weiter.** `SchoolYear` ist eine geteilte, veränderliche Entität, die
`SchoolYearResource.update()` im Request-Thread in-place ändert, während Jobs im Job-Thread
laufen. `CsvWriter` legt die Spaltenzahl aus dem Header-Stream fest und bricht Zeilen allein durch
Zählen der `append()`-Aufrufe um. Ein Flip zwischen zwei getrennten Lesevorgängen würde den Export
nicht abbrechen, sondern **jede Datenzelle stillschweigend um eine bzw. drei Spalten verschieben**
(FR-008, FR-009).

### 3.8 `job/PayrollCSVDownload.java`

- `createHeaders(boolean showIpbBalances)`: `"IPB-Saldo Ende SJ"` bedingt.
- `run()`: neues lokales `showIpbBalances` neben dem bestehenden `calculationModeIsLessons2`;
  `csv.append(roundPercent(workload.getClosingBalance()))` bedingt.

Der Berechnungsmodus wird in diesem Job weiterhin zweimal gelesen (`run()` `:66`,
`createHeaders()`) und ist demselben Risiko ausgesetzt. Das ist vorbestehend und hier bewusst
nicht mit angefasst — der Modus zu ändern erfordert ohnehin eine Neuberechnung.

**Die IPB-Korrektur-Blöcke bleiben ausschliesslich an `calculationModeIsLessons2` gekoppelt**
(FR-016, AC-7). Ein Kommentar über der Modus-Abfrage in `createHeaders()` hält das fest — die
Verwechslung der beiden Konzepte ist der wahrscheinlichste Fehler bei künftigen Änderungen.

## 4. Frontend (`pensen-app`)

### 4.1 `views/SchoolYear/SchoolYearEdit.vue`

Checkbox „IPB-Saldi anzeigen" unterhalb von „archiviert", **mit** `v-if="!add"` wie `finalised`
und `archived`.

*Abweichung vom ursprünglichen Plan:* Da der Server den Wert beim Anlegen vom letzten Schuljahr
erbt (3.3), würde eine Checkbox im Add-Dialog entweder einen falschen Vorgabewert zeigen oder die
Vererbungsregel im Frontend duplizieren. Das Feld wird beim Anlegen darum nicht gesendet und ist
erst im Bearbeiten-Dialog sichtbar. Die Regel liegt so an genau einer Stelle.

### 4.2 `views/Employment/Employment.vue`

Eine `computed`-Property `showIpbBalances` (`!this.schoolYear || this.schoolYear.showIpbBalances
!== false`) trägt die Bedingung für beide Anpassungen dieser Ansicht.

Der Vergleich auf `=== false` statt auf Falsy ist beabsichtigt: solange das Schuljahr noch nicht
geladen ist (`schoolYear: {}`), soll die Ansicht ihr bisheriges Verhalten zeigen.

**Spalten.** Die Liste in `data()` heisst neu `allHeaders`; die `computed`-Property `headers`
filtert die Einträge `openingBalance`, `change` und `closingBalance` heraus. Der Template-Ausdruck
`:headers="headers"` bleibt unverändert. Die Spaltennamen stehen in der Modulkonstante
`IPB_BALANCE_COLUMNS`.

**Zeilen-Einfärbung (FR-018).** `rowClass(item)` (`:201-217`) färbt Zeilen nach `item.change`
grün oder rot ein. Die Methode erhält als erste Anweisung `if (!this.showIpbBalances) return '';`.

Das ist keine Kosmetik: ohne diesen Riegel ergibt `Math.abs(undefined)` den Wert `NaN`, alle drei
Schwellenwertvergleiche werden `false`, und die Methode läuft bis `'green lighten-2'` durch — die
gesamte Liste wäre grün eingefärbt. Ein weggelassenes Feld führt hier zu einer *falschen*, nicht
zu einer *fehlenden* Anzeige.

### 4.3 `views/Overview/Overview.vue` und `views/Overview/DivisionTable.vue`

- `DivisionTable.vue`: neues Prop `showIpbBalances` (Boolean, Vorgabe `true`). Die Header-Liste ist
  von `data()` in eine `computed`-Property gewandert und hängt „IPB-Guthaben" nur bedingt an; die
  Summenzelle in `body.append` hat ein `v-if`.
- `Overview.vue`: reicht `!schoolYear || schoolYear.showIpbBalances !== false` als Prop durch.
- Beide Summenbildungen (`Overview.vue` `sumEmployment`, `DivisionTable.vue` `watch.items`) sind
  mit `|| 0` gegen fehlende Felder abgesichert, damit kein `NaN` entsteht.

### 4.4 Keine Änderung nötig

- `views/Workload/Workload.vue:32` — `<Balance v-if="workload" :data="workload.balance" />`
  zusammen mit `Balance.vue:2` (`<div v-if="data">`) blendet den Abschnitt aus, sobald das Feld
  fehlt (FR-014). `Balance.vue` darf darum kein `default: []` erhalten.
- `views/Workload/Postings.vue` — `Workload.vue:27-30` prüft bereits `workload.postings`; der
  Entscheid 9.1 ist damit rein serverseitig umgesetzt.

## 5. Verifikation

**Durchgeführt**

- `mvn -o compile` läuft fehlerfrei.
- Code-Prüfung aller sieben Anzeigestellen aus spec.md Abschnitt 7.

**Offen**

- Der Frontend-Build wurde **nicht** ausgeführt: `node_modules` in `pensen-app` ist nicht
  installiert (`@vue/cli-service` und `eslint` fehlen). Vor dem Zusammenführen sind
  `npm install` und `npm run lint` bzw. `npm run build` nachzuholen.
- Manuelle Prüfung: PDF-Ausgabe (AC-1), Frontend-Ansichten (AC-3, AC-4), Migration gegen eine
  Kopie der Produktivdatenbank (AC-11), Wiedereinschalten (AC-10), gemischte Schuljahre (AC-9).

**Automatisierte Tests** sind derzeit nicht möglich: das Repository enthält kein `src/test` und
keine Test-Abhängigkeit in `pom.xml`. Sinnvolle Kandidaten, sobald JUnit 5 und
`maven-surefire-plugin` eingerichtet sind:

- `Employment.toJsonTerse()` mit Flag `true` und `false` — Feldmenge (AC-3)
- `Workload.toJsonTerse()` mit Flag `true` und `false` — Schlüssel `balance` und `postings` (AC-2)
- `createHeaders()` beider CSV-Jobs — Spaltenanzahl Kopfzeile gegen Datenzeile (AC-5, AC-6)
- `PayrollCSVDownload` mit `showIpbBalances = false` und `calculationMode = lessons2` — die
  `S IPBKorr`-Spalten müssen bleiben (AC-7)