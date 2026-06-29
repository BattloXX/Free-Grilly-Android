# Free-Grilly Android

Native Android-App für das **Free-Grilly** Grillthermometer (BattloXX-Fork der Grilleye-Max-Firmware, ESP32).

[![Release](https://img.shields.io/github/v/release/BattloXX/Free-Grilly-Android)](https://github.com/BattloXX/Free-Grilly-Android/releases/latest)
[![Min SDK](https://img.shields.io/badge/Android-8.0%2B-green)](https://github.com/BattloXX/Free-Grilly-Android)

> **Firmware:** Diese App funktioniert mit der [BattloXX/free-grilly](https://github.com/BattloXX/free-grilly) Firmware.

---

## Installation

1. Die neueste APK von der [Releases-Seite](https://github.com/BattloXX/Free-Grilly-Android/releases/latest) herunterladen (`free-grilly-v*.apk`).
2. Auf dem Android-Gerät: *Einstellungen → Apps → Installation unbekannter Quellen* erlauben.
3. APK installieren.

> Die App prüft beim Start automatisch auf neue Versionen und bietet ein In-App-Update an.

---

## Features

- **Automatische Geräteerkennung** via mDNS (`_free-grilly._tcp`) — kein manuelles IP-Eintippen nötig
- **Live-Temperaturen** aller bis zu 8 Sonden (1-Sekunden-Polling)
- **Verlaufs-Graph** (Compose Canvas, bis zu 10 Minuten)
- **Alarm-Benachrichtigungen** wenn eine Sonde die Zieltemperatur erreicht (auch im Hintergrund)
- **Einrichtungs-Assistent** (AP-Provisioning + WLAN-Konfiguration)
- **Grillgut-Bibliothek** mit kuratierten Kerntemperaturen (Rind, Schwein, Geflügel, Fisch, Lamm, Wild)
- **Smartphone & Tablet** – adaptives Layout (BottomBar / NavigationRail)
- **Zweisprachig** DE/EN (System-Locale + In-App-Umschaltung)
- **Demo-Modus** – ohne Hardware bedienbar
- Eigene Grillgüter + Favoriten (Room-Datenbank)
- **In-App OTA** – Firmware-Update direkt aus der App heraus (wenn vom Gerät unterstützt)

## Voraussetzungen

| | |
|---|---|
| **Gerät** | Free-Grilly (Grilleye Max mit [BattloXX-Firmware](https://github.com/BattloXX/free-grilly/releases/latest)) |
| **Android** | 8.0 Oreo (API 26) oder neuer |
| **Netzwerk** | Gerät und Smartphone im selben WLAN |

## Tech-Stack

- Kotlin · Jetpack Compose (Material 3)
- Retrofit 2 + OkHttp + Kotlinx Serialization
- Hilt (DI) · Room · DataStore
- Compose Canvas (Verlaufs-Graph)
- `NsdManager` (mDNS-Geräteerkennung)
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

Die Firmware stellt eine lokale HTTP-REST-API bereit. Details: [`docs/android_app.md`](https://github.com/BattloXX/free-grilly/blob/master/docs/android_app.md) im Firmware-Repo.

---

# Free-Grilly Android (English)

Native Android app for the **Free-Grilly** grill thermometer (BattloXX fork of the Grilleye Max firmware, ESP32).

[![Release](https://img.shields.io/github/v/release/BattloXX/Free-Grilly-Android)](https://github.com/BattloXX/Free-Grilly-Android/releases/latest)

> **Firmware:** This app works with the [BattloXX/free-grilly](https://github.com/BattloXX/free-grilly) firmware.

## Installation

1. Download the latest APK from the [Releases page](https://github.com/BattloXX/Free-Grilly-Android/releases/latest) (`free-grilly-v*.apk`).
2. On your Android device: allow installation from unknown sources (*Settings → Apps → Install unknown apps*).
3. Install the APK.

> The app checks for new versions on startup and offers an in-app update.

## Features

- **Automatic device discovery** via mDNS (`_free-grilly._tcp`) — no manual IP entry required
- **Live temperatures** for up to 8 probes (1-second polling)
- **History graph** (Compose Canvas, up to 10 minutes)
- **Alarm notifications** when a probe reaches its target temperature (including in the background)
- **Setup wizard** (AP provisioning + Wi-Fi configuration)
- **Food library** with curated target temperatures (beef, pork, poultry, fish, lamb, game)
- **Phone & Tablet** – adaptive layout (BottomBar / NavigationRail)
- **Bilingual** DE/EN (system locale + in-app toggle)
- **Demo mode** – usable without hardware
- Custom food entries + favorites (Room database)
- **In-app OTA** – update device firmware directly from the app (when supported by firmware)

## Requirements

| | |
|---|---|
| **Device** | Free-Grilly (Grilleye Max with [BattloXX firmware](https://github.com/BattloXX/free-grilly/releases/latest)) |
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

The firmware exposes a local HTTP REST API. Details: [`docs/android_app.md`](https://github.com/BattloXX/free-grilly/blob/master/docs/android_app.md) in the firmware repo.
