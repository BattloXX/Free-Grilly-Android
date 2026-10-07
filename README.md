# Free-Grilly Android

Native Android-App für das **Free-Grilly** Grillthermometer (BattloXX-Fork der Grilleye-Max-Firmware, ESP32).

[![Release](https://img.shields.io/github/v/release/BattloXX/Free-Grilly-Android)](https://github.com/BattloXX/Free-Grilly-Android/releases/latest)
[![Min SDK](https://img.shields.io/badge/Android-8.0%2B-green)](https://github.com/BattloXX/Free-Grilly-Android)

> **Firmware:** Diese App funktioniert mit [BattloXX/free-grilly](https://github.com/BattloXX/free-grilly) und [Grilly+](https://github.com/bardesss/grilly-plus). Die Gerätesuche, Live-Werte, Verlauf und OTA wählen die passende Firmware-API automatisch.

> ⚠️ **Hinweis zu Google Play Protect:** Beim Installieren stuft Android/Play Protect die APK möglicherweise als „schädlich" ein. Grund ist nicht der tatsächliche Code, sondern das Verhaltensmuster: Die App wird außerhalb des Play Stores verteilt (Sideload) *und* lädt bei Updates selbstständig eine neue APK herunter und bietet sie zur Installation an (`REQUEST_INSTALL_PACKAGES`) — genau dieses Muster nutzt Play Protect als Heuristik für Dropper/Trojaner, unabhängig davon, was die App wirklich tut. Der Quellcode ist hier vollständig einsehbar; die App kommuniziert ausschließlich mit dem Grillgerät im lokalen WLAN und mit der GitHub-API (für Update-Checks). Wer unsicher ist, kann den Code selbst prüfen/bauen oder den Fehlalarm [bei Google melden](https://support.google.com/googleplay/protect/answer/9059445).

---

## Installation

1. Die neueste APK von der [Releases-Seite](https://github.com/BattloXX/Free-Grilly-Android/releases/latest) herunterladen (`free-grilly-v*.apk`).
2. Auf dem Android-Gerät: *Einstellungen → Apps → Installation unbekannter Quellen* erlauben.
3. APK installieren.

> Die App prüft beim Start automatisch auf neue Versionen und bietet ein In-App-Update an.

---

## Features

- **Automatische Geräteerkennung** via mDNS (`_free-grilly._tcp`, `_grilly-plus._tcp`, `_grilly._tcp`), jedes Gerät wird per `GET /api/info` bestätigt und über seine UUID (nicht die IP) identifiziert — kein manuelles IP-Eintippen nötig
- **Eine App für Free-Grilly und Grilly+** — die Firmware-Unterschiede stecken in einer Adapter-Schicht, die UI arbeitet auf einem gemeinsamen Modell; Funktionen werden über die `capabilities` aus `/api/info` freigeschaltet, nicht über Versionsnummern
- **Live-Temperaturen** aller bis zu 8 Sonden (1-Sekunden-Polling)
- **Verlaufs-Graph** (Compose Canvas) mit lokal gespeicherter Historie; bei Grilly+ werden feine und grobe Verlaufsstufe beim Verbinden geladen und zusammengeführt, sodass auch mehrstündige Cooks nach dem Schließen der App ihren Graphen behalten
- **Alarm-Benachrichtigungen** pro Sonde nach dem Alarmzustand der Firmware (auch im Hintergrund), mit den Aktionen **Stumm** und **Öffnen**
- **Einrichtungs-Assistent** (AP-Provisioning + WLAN-Konfiguration), erkennt die Firmware über `/api/info` (AP-Namen `FreeGrilly_…`, `GrillyPlus_…`, `Grilleye…`) und wartet nach dem Setup begrenzt auf das Gerät im Heim-WLAN
- **Grillgut-Bibliothek** mit kuratierten Kerntemperaturen (Rind, Schwein, Geflügel, Fisch, Lamm, Wild)
- **Smartphone & Tablet** – adaptives Layout (BottomBar / NavigationRail)
- **Zweisprachig** DE/EN (System-Locale + In-App-Umschaltung)
- **Demo-Modus** – ohne Hardware bedienbar
- Eigene Grillgüter + Favoriten (Room-Datenbank)
- **In-App OTA** – Firmware-Update direkt aus der App heraus (wenn vom Gerät unterstützt); bei gesetztem Admin-Passwort (Grilly+) fragt die App danach, auf Wunsch wird es per Android Keystore verschlüsselt gemerkt
- **Sonden-Kalibrierung** (Offset in °C) – nur sichtbar, wenn die Firmware `calibration_offset` meldet
- **Geräte-Status & Diagnose** – alle von der Firmware gelieferten Angaben (Firmware-Name/-Version, API-Version, Akku %, Zellspannung, Laden, WLAN-Signal, Laufzeit, Grund des letzten Neustarts / der letzten Abschaltung, UUID, mDNS-Hostname); unbekannte Felder werden ausgeblendet

## Voraussetzungen

| | |
|---|---|
| **Gerät** | Grilleye Max mit [Free-Grilly](https://github.com/BattloXX/free-grilly/releases/latest) oder [Grilly+](https://github.com/bardesss/grilly-plus) Firmware |
| **Android** | 8.0 Oreo (API 26) oder neuer |
| **Netzwerk** | Gerät und Smartphone im selben WLAN |

## Tech-Stack

- Kotlin · Jetpack Compose (Material 3)
- Retrofit 2 + OkHttp + Kotlinx Serialization
- Hilt (DI) · Room · DataStore
- Compose Canvas (Verlaufs-Graph)
- `NsdManager` (mDNS-Geräteerkennung), Android Keystore (optional gemerktes OTA-Passwort)
- `NotificationCompat` + Foreground-Service (Hintergrund-Polling)

## Bauen

> **Voraussetzungen:** JDK 17, Android SDK (API 35)

```bash
./gradlew assembleDebug
```

Das Debug-APK liegt unter `app/build/outputs/apk/debug/`.

### CI / Release

- **CI** (`android-ci.yml`): Build + Lint + Tests bei jedem Push auf `main`
- **Release** (`release.yml`): Signiertes Release-APK wird automatisch erstellt wenn ein `v*.*.*`-Tag gepusht wird

## API

Die Firmware stellt eine lokale HTTP-REST-API bereit. Details: [`docs/android_app.md`](https://github.com/BattloXX/free-grilly/blob/master/docs/android_app.md) im Firmware-Repo (Free-Grilly) bzw. `docs/openapi.yaml` im [Grilly+-Repo](https://github.com/bardesss/grilly-plus).

Intern greift die App über `GrillyDeviceApi` mit `FreeGrillyApiAdapter` bzw. `GrillyPlusApiAdapter` auf das Gerät zu; der Adapter wird beim Verbinden aus `/api/info` gewählt. Bereits vorbereitet, aber erst aktiv, wenn die Firmware es meldet: Cook-Sessions nach Firmware-ID (`cook_session`), Mute pro Sonde (`alarm_probe_mute`) und der SSE-Stream (`events`).

---

# Free-Grilly Android (English)

Native Android app for the **Free-Grilly** grill thermometer (BattloXX fork of the Grilleye Max firmware, ESP32).

[![Release](https://img.shields.io/github/v/release/BattloXX/Free-Grilly-Android)](https://github.com/BattloXX/Free-Grilly-Android/releases/latest)

> **Firmware:** This app works with [BattloXX/free-grilly](https://github.com/BattloXX/free-grilly) and [Grilly+](https://github.com/bardesss/grilly-plus), automatically selecting the matching device API.

> ⚠️ **Note on Google Play Protect:** Android/Play Protect may flag the APK as "harmful" during install. This isn't about the actual code — it's a behavioral pattern match: the app is distributed outside the Play Store (sideload) *and* downloads a new APK on update and offers it for installation (`REQUEST_INSTALL_PACKAGES`) — exactly the pattern Play Protect's heuristics use to catch droppers/trojans, regardless of what the app actually does. The source is fully available here; the app only talks to the grill device on the local Wi-Fi and to the GitHub API (for update checks). If in doubt, review/build the code yourself, or [report the false positive to Google](https://support.google.com/googleplay/protect/answer/9059445).

## Installation

1. Download the latest APK from the [Releases page](https://github.com/BattloXX/Free-Grilly-Android/releases/latest) (`free-grilly-v*.apk`).
2. On your Android device: allow installation from unknown sources (*Settings → Apps → Install unknown apps*).
3. Install the APK.

> The app checks for new versions on startup and offers an in-app update.

## Features

- **Automatic device discovery** via mDNS (`_free-grilly._tcp`, `_grilly-plus._tcp`, `_grilly._tcp`); every device is confirmed with `GET /api/info` and identified by its UUID (not its IP) — no manual IP entry required
- **One app for Free-Grilly and Grilly+** — firmware differences live in an adapter layer, the UI works on a common model; features are enabled by the `capabilities` from `/api/info`, not by version numbers
- **Live temperatures** for up to 8 probes (1-second polling)
- **History graph** (Compose Canvas) with locally persisted history; on Grilly+ the fine and coarse history tiers are loaded and merged on connect, so multi-hour cooks keep their graph after the app was closed
- **Alarm notifications** per probe based on the firmware's alarm state (including in the background), with **Mute** and **Open** actions
- **Setup wizard** (AP provisioning + Wi-Fi configuration) that detects the firmware via `/api/info` (AP names `FreeGrilly_…`, `GrillyPlus_…`, `Grilleye…`) and waits a bounded time for the device on your home Wi-Fi
- **Food library** with curated target temperatures (beef, pork, poultry, fish, lamb, game)
- **Phone & Tablet** – adaptive layout (BottomBar / NavigationRail)
- **Bilingual** DE/EN (system locale + in-app toggle)
- **Demo mode** – usable without hardware
- Custom food entries + favorites (Room database)
- **In-app OTA** – update device firmware directly from the app (when supported by firmware); if an admin password is set (Grilly+) the app asks for it and can remember it encrypted with the Android Keystore if you choose
- **Probe calibration** (offset in °C) — only shown when the firmware reports `calibration_offset`
- **Device status & diagnostics** – everything the firmware provides (firmware name/version, API version, battery %, cell voltage, charging, Wi-Fi signal, uptime, reason for the last restart / last power-off, UUID, mDNS hostname); unknown fields are hidden

## Requirements

| | |
|---|---|
| **Device** | Grilleye Max with [Free-Grilly](https://github.com/BattloXX/free-grilly/releases/latest) or [Grilly+](https://github.com/bardesss/grilly-plus) firmware |
| **Android** | 8.0 Oreo (API 26) or newer |
| **Network** | Device and phone on the same Wi-Fi network |

## Building

> **Requirements:** JDK 17, Android SDK (API 35)

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/`.

### CI / Release

- **CI** (`android-ci.yml`): Build, lint and tests on every push to `main`
- **Release** (`release.yml`): Signed release APK is built and published automatically when a `v*.*.*` tag is pushed

## API

The firmware exposes a local HTTP REST API. Details: [`docs/android_app.md`](https://github.com/BattloXX/free-grilly/blob/master/docs/android_app.md) in the firmware repo (Free-Grilly) and `docs/openapi.yaml` in the [Grilly+ repo](https://github.com/bardesss/grilly-plus).

Internally the app talks to the device through `GrillyDeviceApi` with a `FreeGrillyApiAdapter` or `GrillyPlusApiAdapter`, chosen on connect from `/api/info`. Prepared but only active once the firmware reports it: cook sessions by firmware ID (`cook_session`), per-probe mute (`alarm_probe_mute`) and the SSE stream (`events`).
