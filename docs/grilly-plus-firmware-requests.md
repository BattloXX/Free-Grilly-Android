# Grilly+ firmware extensions the Android app is ready for

The Free-Grilly Android app treats Free-Grilly and Grilly+ as one device type. It
selects an adapter from `GET /api/info` and enables features from the `capabilities`
array — **never from version numbers**. The features below are already implemented in
the app and stay dormant until the firmware advertises them, so each one can be added to
Grilly+ independently and without breaking existing clients.

Everything here is additive. A firmware that adds none of it keeps working exactly as today.

## Status

Implemented in [BattloXX/free-grilly](https://github.com/BattloXX/free-grilly) branch
`grilly-plus` (PRs #15, #16): `cook_session`, `alarm_probe_mute`, `ota_auth`, and
`uptime_seconds` (the field name is `uptime_seconds`). `GET /api/events` is implemented,
but the `events` capability is not yet advertised pending hardware testing.

## Current baseline (what the app already uses)

`GET /api/info` → `firmware`, `firmware_version`, `api_version`, `unique_id`, `hostname`,
`probe_count`, `capabilities` (`history`, `eta`, `clear_history`, `alarm_mute`,
`alarm_probes`, `calibration_offset`, `diagnostics`, `ota_upload`). Also used: `/api/grill`,
`/api/probes`, `/api/settings`, `/api/history[?probe=N]`, `/api/history/clear`,
`/api/alarm/mute`, `/api/wifiscan`, `POST /api/update` (`X-Grilly-Update: 1`, Basic `admin`).

Notes on what the app does with that baseline:

- It loads `GET /api/history?probe=N` **per connected probe** to get both the fine and coarse
  tier (the call without `probe` returns only the coarse tier).
- `calibration_offset` already enables the probe calibration editor (`offset_celcius`).
- `alarm_probes` gives per-probe `alarm`; the app only notifies for probes the firmware flags.

## Requested additions

### 1. Cook session id — `GET /api/grill`

Lets the app group history per cook and never append a new cook to an old graph.

```json
{ "cook_session": { "id": "c-26100114-0007" } }
```

- `id`: opaque string, **stable for the whole cook**, **changes when a new cook starts**
  (e.g. history cleared, first probe connected after idle, reboot that loses history).
  Must not change merely because the app reconnects or the device re-joins Wi-Fi.
- The app also accepts a flat `"cook_session_id": "..."`.
- No capability needed; the app reads the field when present. Optional suggestion: add a
  `cook_session` capability so clients can tell "absent" from "not supported".
- Without it the app falls back to "same cook if the last sample is < 60 min old".

### 2. Per-probe mute — capability `alarm_probe_mute`

```
POST /api/probes/{id}/alarm/mute      {}      → 200 {"success": true}
```

Mutes only that probe's alarm (the notification's **Mute** action targets one probe).
Advertise `alarm_probe_mute` in `capabilities`. Without it the app calls `POST /api/alarm/mute`.

### 3. Push events — capability `events` (alias `sse` also accepted)

```
GET /api/events        Accept: text/event-stream
```

Each SSE `data:` frame is a JSON object with the **same shape as `GET /api/grill`**
(partial frames are not supported yet). Send a frame on every change and at least every
few seconds as keep-alive. The app uses the stream while open, reconnects with back-off and
falls back to 1 s polling of `/api/grill` if the stream fails. The device's web server is
single-threaded, so a lightweight single-client stream is fine.

### 4. OTA authentication hint — capability `ota_auth`

Advertise `ota_auth` **when an admin password is currently set**. Then the app asks for it
*before* uploading. Without the capability the app uploads first and asks only after an
HTTP 401 (`Wrong admin password`), so this is a UX nicety, not a requirement.

### 5. Optional diagnostics fields — `GET /api/grill`

Shown on the status screen when present, hidden otherwise:
`wifi_signal` (dBm), `battery_percentage`, `battery_millivolts`, `battery_charging`,
`last_reset_reason`, `last_off_reason`, `uptime_seconds` (integer seconds since boot).

## Not needed

- `/api/provision`: the setup wizard uses `POST /api/settings` (`wifi_ssid`, `wifi_password`,
  `name`, `temperature_unit`) on the setup AP at `192.168.200.10`, which works today.
- A separate "long-term history" capability: `history` with `fine` + `coarse` tiers is enough.

## mDNS

The app browses `_free-grilly._tcp`, `_grilly-plus._tcp` and `_grilly._tcp` and confirms each
hit with `GET /api/info`; the device UUID (`unique_id`) is the permanent identifier, not the IP.
Advertising any one of these service types works.

## Checklist for firmware authors

| Request | Field / endpoint | Capability |
|---|---|---|
| Cook session id | `cook_session.id` in `/api/grill` | (optional) |
| Per-probe mute | `POST /api/probes/{id}/alarm/mute` | `alarm_probe_mute` |
| Push events | `GET /api/events` (SSE, `/api/grill`-shaped frames) | `events` |
| OTA auth hint | — | `ota_auth` |
| Uptime | `uptime_seconds` in `/api/grill` | — |
