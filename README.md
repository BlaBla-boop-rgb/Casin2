# Craft Casino – Paper-Plugin für 1.21.11

`/casino` (oder `/ccasino`) öffnet ein animiertes Truhen-Casino mit vier Tischen:

| Spiel | Was passiert |
|---|---|
| **Slots** | 3 Walzen rollen nach unten und stoppen nacheinander, Hebel zum Ziehen, LED-Gewinnanzeige |
| **Roulette** | Ein Band aus Kesselzahlen läuft unter dem Zeiger durch und rollt aus; Rot/Schwarz, Gerade/Ungerade, 1–18/19–36, Dutzende, 0 und Einzelzahlen (eigenes Zahlenfeld) |
| **Blackjack** | Karten werden nacheinander ausgeteilt, die Dealerkarte wird aufgedeckt; Karte / Halten / Doppeln |
| **Crash** | Rakete steigt durch den Sternenhimmel, LED-Multiplikator, rechtzeitig auszahlen |

Lauflichter oben (bei Gewinnen bunt blinkend), Einsatz-Chips unten, Sounds und Partikel.
Gespielt wird mit dem **Vault-Guthaben** des Servers.

## Installation

1. **Vault** und ein Economy-Plugin (z. B. EssentialsX) müssen auf dem Server laufen.
2. `CraftCasino-1.0.0.jar` in den `plugins`-Ordner, Server neu starten.
3. `CasinoPack.zip` aus diesem Repo öffentlich erreichbar machen, z. B. über den GitHub-Raw-Link:
   `https://raw.githubusercontent.com/<Name>/<Repo>/main/CasinoPack.zip`
4. Den Link in `plugins/CraftCasino/config.yml` bei `resource-pack.url` eintragen, dann `/casino reload`.

Das Pack wird **zusätzlich** zu deinem Server-Resourcepack geschickt (z. B. ClansPack) – beide bleiben aktiv.
Spieler, die das Pack ablehnen, sehen das Casino im Vanilla-Look (Glasscheiben, normale Items).

## Befehle & Rechte

| Befehl | Recht | |
|---|---|---|
| `/casino` | `casino.use` (Standard: alle) | Casino öffnen |
| `/casino pack` | `casino.use` | Casino-Pack erneut laden |
| `/casino reload` | `casino.admin` (OP) | config.yml neu laden |

## Bauen

GitHub Actions baut automatisch (`.github/workflows/build.yml`) – die Jar liegt danach unter
**Actions → letzter Lauf → Artifacts**. Lokal: JDK 21, `./gradlew build` → `build/libs/`.

## Resourcepack neu erzeugen

Alle Texturen sind selbst gezeichnet und werden per Skript erzeugt:

```
pip install pillow
python3 tools/make_pack.py   ->  pack/ und CasinoPack.zip
```
