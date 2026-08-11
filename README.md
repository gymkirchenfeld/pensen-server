# Applikationsserver für den neuen Pensenmanager

[Dokumentation](https://docs.gymkirchenfeld.ch/pensen)

## Lokale Entwicklung

Vorausgesetzt sind ein JDK (Zielversion ist Java 11) und Maven. Das Projekt
hängt von zwei Bibliotheken ab, die auf **keinem** Maven-Repository liegen:

| Abhängigkeit | Repository |
| --- | --- |
| `ch.kinet:ch.kinet.datalib:1.0.0` | [gymkirchenfeld/ch.kinet.datalib](https://github.com/gymkirchenfeld/ch.kinet.datalib) |
| `ch.kinet:ch.kinet.pdflib:1.0.0` | [gymkirchenfeld/ch.kinet.pdflib](https://github.com/gymkirchenfeld/ch.kinet.pdflib) |

Beide müssen **neben** diesem Projekt ausgecheckt sein, damit `build.sh` sie
findet:

```
dev/
├── ch.kinet.datalib/
├── ch.kinet.pdflib/
└── pensen-server/
```

```sh
./build.sh
```

| Befehl | Zweck |
| --- | --- |
| `./build.sh` | Baut beide Bibliotheken und anschliessend dieses Projekt |
| `mvn package` | Baut nur dieses Projekt (die Bibliotheken müssen im lokalen Maven-Repository liegen) |
| `./serve.sh` | Startet den Server lokal gegen `application.cfg` |

Das Ergebnis ist `target/pensen-server-jar-with-dependencies.jar` — ein Fat Jar
mit allen Abhängigkeiten.

`application.cfg` ist **nicht** eingecheckt und enthält unter anderem das
Datenbankpasswort. Für eine neue Arbeitsumgebung muss die Datei von Hand
angelegt werden. Das Datenbankschema liegt unter `db/create.sql`, Änderungen
daran unter `db/migration/`.

## Was beim Push nach GitHub passiert

Drei Workflows unter `.github/workflows/`. Welcher läuft, hängt vom Ereignis ab:

| Ereignis | `ci.yml` | `staging.yml` | `release.yml` |
| --- | --- | --- | --- |
| Push auf einen Feature-Branch (ohne offenen PR) | — | — | — |
| Pull Request geöffnet oder aktualisiert | ✅ | — | — |
| Push/Merge nach `main` | ✅ | — | — |
| Manueller Start über die Actions-Oberfläche | — | ✅ | — |
| Push eines Tags `v*` | — | — | ✅ |

Ein Push auf einen Feature-Branch ohne offenen PR löst also nichts aus. Sobald
ein PR offen ist, läuft die CI bei jedem weiteren Push auf diesen Branch.

### Versions-Check in jedem Lauf

Der Server führt seine Version an **zwei** Stellen: in `pom.xml` und in
`src/main/java/ch/kinet/pensen/server/Version.java`. Die beiden Angaben müssen
übereinstimmen — sonst meldet sich ein Artefakt im Betrieb unter einer anderen
Version als der, aus der es gebaut wurde.

Darum prüft `.github/scripts/check-version.sh` in **allen drei** Workflows, dass
die beiden Angaben übereinstimmen. Im Release-Workflow wird zusätzlich der Tag
gegen beide geprüft. Der Check läuft jeweils als erster Schritt nach dem
Auschecken — er braucht nur das auf dem Runner vorinstallierte Maven und
schlägt so nach Sekunden fehl statt nach dem Bau der Bibliotheken.

Lokal lässt er sich genauso aufrufen:

```sh
bash .github/scripts/check-version.sh          # pom.xml gegen Version.java
bash .github/scripts/check-version.sh 3.10.0   # zusätzlich gegen eine erwartete Version
```

### Die Bibliotheken in der Pipeline

Weil die beiden `ch.kinet`-Bibliotheken auf keinem Maven-Repository liegen, kann
ein Runner sie nicht herunterladen. Die Composite Action
`.github/actions/setup-build` richtet darum das JDK ein, checkt beide
Repositories aus und installiert sie mit `mvn install` in das lokale
Maven-Repository des Runners — sie tut also dasselbe wie `build.sh`.

Gebaut wird jeweils der Stand von `main`. Beide Bibliotheken tragen dauerhaft
die Version `1.0.0`, entwickeln sich darunter aber weiter; ihre `v1.0.0`-Tags
bilden einen deutlich älteren Stand ab als den, gegen den `pensen-server`
kompiliert. `main` entspricht dem, was lokal in den Nachbarordnern liegt.

Das bedeutet zugleich: Ein Build ist nicht vollständig reproduzierbar, weil eine
Änderung an einer Bibliothek einen späteren Build desselben Commits verändern
kann. Wer das ausschliessen will, ersetzt `ref: main` in
`.github/actions/setup-build/action.yml` durch einen festen Commit-SHA — dann
muss er dort bei jeder Bibliotheksänderung nachgeführt werden.

### CI (`ci.yml`)

Läuft bei Pull Requests und bei Pushes nach `main`:

1. Repository auschecken
2. **Versions-Check** (siehe oben)
3. JDK 21 einrichten, Maven-Cache wiederherstellen, die beiden Bibliotheken
   bauen und installieren
4. `mvn verify`

Schlägt einer der Schritte fehl, wird der PR rot markiert. Es wird nichts
veröffentlicht und nichts deployt.

Beachte: Das Projekt hat derzeit kein `src/test` und keine Test-Abhängigkeit in
der `pom.xml`. `mvn verify` prüft damit im Moment nur, dass das Projekt
kompiliert. Sobald JUnit eingerichtet ist, laufen Tests ohne Änderung am
Workflow mit.

### Staging (`staging.yml`)

Stellt einen Teststand für den Auftraggeber bereit, **bevor** ein Tag gesetzt
wird. Der Workflow wird von Hand gestartet: *Actions → Staging → Run workflow*,
dort im Dropdown den gewünschten Branch wählen. Optional lässt sich eine Notiz
mitgeben, die in der Beschreibung des Prereleases landet.

Gebaut wird der Stand des gewählten Branches. Ergebnis ist ein als
*Pre-release* markiertes Release mit dem Tag `staging-<branch>-<sha>`, an dem
das Fat Jar als `.zip` und `.tar.gz` hängt. Weil das Repository öffentlich ist,
sind die Asset-Links ohne Anmeldung ladbar und können direkt weitergegeben
werden.

Der Titel des Prereleases enthält den Zeitpunkt des Builds in Schweizer Zeit,
etwa `Staging: 001-show-ipb-balances - 11.08.2026 16:42`. Die Releases-Seite
sortiert nämlich nach dem zugrunde liegenden Tag und nicht nach dem Zeitpunkt
des Workflow-Laufs — am Titel erkennst du zuverlässig, welcher Eintrag der
aktuelle ist.

Der Namensraum ist bewusst von den Release-Tags getrennt: `release.yml` reagiert
nur auf `v*`, ein `staging-*`-Tag löst dort also nichts aus. Der SHA im Namen
macht jeden Teststand eindeutig nachvollziehbar. Ein erneuter Lauf auf demselben
Commit ersetzt lediglich die Assets des bestehenden Prereleases.

Prereleases sammeln sich mit der Zeit an — alte Einträge können unter *Releases*
von Hand gelöscht werden (samt Tag über *Delete tag*).

### Release (`release.yml`)

Läuft ausschliesslich bei Tags, die mit `v` beginnen:

1. Auschecken
2. **Versions-Check**: Der Tag ohne führendes `v` wird gegen `pom.xml` und
   `Version.java` geprüft. Bei `v3.10.0` muss in beiden `3.10.0` stehen, sonst
   bricht der Workflow ab und es entsteht kein Release.
3. JDK und Bibliotheken einrichten (wie oben)
4. `mvn clean package`
5. Das Fat Jar wird in zwei Formaten gepackt: `pensen-server-<tag>.zip` und
   `pensen-server-<tag>.tar.gz`
6. `gh release create --draft` legt ein GitHub-Release zum Tag an, hängt beide
   Archive als Assets an und generiert die Release-Notes aus den Commits seit
   dem letzten Release

Das Release entsteht als **Entwurf** und ist damit zunächst nur für Personen mit
Schreibrechten sichtbar. Titel und Beschreibung lassen sich unter
*Releases → Edit release* frei überarbeiten — die generierten Notes sind nur ein
Startwert. Veröffentlicht wird es erst mit einem Klick auf *Publish release*.

### Inhalt der Archive

Anders als beim Web-Client enthalten die Archive **keinen** Ordner, sondern die
Jar-Datei direkt im Wurzelverzeichnis — und zwar unter ihrem Buildnamen:

```
pensen-server-v3.10.0.zip
└── pensen-server-jar-with-dependencies.jar
```

Das ist Absicht: Der Archivname trägt die Version, der Dateiname darin bleibt
stabil. Die Install-Skripte erwarten genau diesen Namen in `target/` und müssen
darum nicht angepasst werden. Da sich das Archiv in das aktuelle Verzeichnis
entpackt, sollte gezielt nach `target/` entpackt werden:

```sh
unzip -o pensen-server-v3.10.0.zip -d target/
```

### Ein Release erstellen

```sh
# 1. pom.xml UND Version.java auf die neue Version setzen und über einen PR nach main
# 2. dann auf main:
git tag v3.10.0
git push origin v3.10.0
```

Passt der Tag nicht zu beiden Versionsangaben, schlägt der Workflow fehl und es
entsteht kein Release. Der Tag bleibt in dem Fall bestehen — er muss lokal und
auf dem Remote gelöscht werden, bevor er korrigiert neu gesetzt werden kann:

```sh
git tag -d v3.10.0
git push origin :refs/tags/v3.10.0
```

## Deployment

Das Deployment ist **nicht** automatisiert. Es läuft weiterhin über die Skripte
im Projektwurzelverzeichnis, die das Jar per `scp` auf den Zielserver kopieren
und dort `install-stage2.sh` ausführen — das Skript stoppt den Dienst, tauscht
die Jar-Datei aus und startet ihn wieder:

| Skript | Ziel | Zielpfad | baut selbst |
| --- | --- | --- | --- |
| `install-kirchenfeld-prod.sh` | `web4.kinet.ch` | `/srv/pensen-server` | ja |
| `install-kirchenfeld-test.sh` | `web4-test.kinet.ch` | `/srv/pensen-server` | ja |
| `install-hofwil.sh` | `pensen.gymhofwil.ch` | `/opt/pensen-server` | ja |
| `install-server-neufeld.sh` | Server als Argument | `/opt/pensen-server` | nein |

Alle Skripte lesen `target/pensen-server-jar-with-dependencies.jar`, kopieren es
nach `target/pensen-server.jar` und laden diese Datei hoch — alle drei
systemd-Units erwarten sie unter diesem Namen. Die Unit-Datei selbst heisst je
nach Instanz unterschiedlich (`kirchenfeld/pensen-api.service`,
`neufeld/pensen-api.service`, `hofwil/pensen.service`) und wird vom Deployment
gleich mit installiert.

Am Gym Neufeld ist der Build vom Deployment getrennt, deshalb erwartet
`install-server-neufeld.sh` ein bereits gebautes Jar und wird mit dem Zielserver
aufgerufen:

```sh
./install-server-neufeld.sh <server>
```

Damit lässt sich dort direkt der Artefakt aus einem Release verwenden, ohne
lokal zu bauen:

```sh
unzip -o pensen-server-v3.10.0.zip -d target/
./install-server-neufeld.sh <server>
```