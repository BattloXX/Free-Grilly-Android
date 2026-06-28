# Free-Grilly Android

Native Android-App für das **Free-Grilly** Grillthermometer (BattloXX-Fork der Grilleye-Max-Firmware, ESP32).

## Features

- **Einrichtungs-Assistent** (AP-Provisioning + WLAN-Konfiguration)
- **Live-Temperaturen** aller bis zu 8 Sonden (1-Sekunden-Polling)
- **Verlaufs-Graph** (Compose Canvas, bis zu 10 Minuten)
- **Alarm-Benachrichtigungen** wenn eine Sonde die Zieltemperatur erreicht
- **Grillgut-Bibliothek** mit kuratierten Kerntemperaturen (Rind, Schwein, Geflügel, Fisch, Lamm, Wild)
- **Smartphone & Tablet** – adaptives Layout (BottomBar / NavigationRail)
- **Zweisprachig** DE/EN (System-Locale + In-App-Umschaltung)
- **Demo-Modus** – ohne Hardware bedienbar
- Eigene Grillgüter + Favoriten (Room-Datenbank)

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

Das Debug-APK liegt anschließend unter `app/build/outputs/apk/debug/`.

### CI

GitHub Actions baut, lintet und testet bei jedem Push auf `main`:
→ `.github/workflows/android-ci.yml`

## Mindest-SDK

`minSdk 26` (Android 8.0 Oreo)

## API

Das Gerät stellt eine lokale HTTP-REST-API bereit. Details: [`docs/android_app.md`](https://github.com/BattloXX/Free-Grilly/blob/main/docs/android_app.md)

---

# Free-Grilly Android (English)

Native Android app for the **Free-Grilly** grill thermometer (BattloXX fork of the Grilleye Max firmware, ESP32).

## Features

- **Setup wizard** (AP provisioning + Wi-Fi configuration)
- **Live temperatures** for up to 8 probes (1-second polling)
- **History graph** (Compose Canvas, up to 10 minutes)
- **Alarm notifications** when a probe reaches its target temperature
- **Food library** with curated target temperatures (beef, pork, poultry, fish, lamb, game)
- **Phone & Tablet** – adaptive layout (BottomBar / NavigationRail)
- **Bilingual** DE/EN (system locale + in-app toggle)
- **Demo mode** – usable without hardware
- Custom food entries + favorites (Room database)

## Building

> **Requirements:** JDK 17, Android SDK (API 35)

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/`.

## Minimum SDK

`minSdk 26` (Android 8.0 Oreo)
