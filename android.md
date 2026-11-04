# Sill for Android — porting the device side

Plan to bring the iPhone/iPad client (`iOSClient/`) to Android. The Mac side
(`Sources/SillHost/`, `StreamProtocol`) is **unchanged**; this covers only the
device/client. Written 2026-11-04.

The work is split into **small lettered packages** (A1, A2, …). Each is a
discrete, reviewable unit (ideally one PR) with explicit dependencies, so many
agents can work in parallel. **Start with the base (A*) and build upward.** A
package may only be started once all of its dependencies are merged. Packages
with no mutual dependencies can be picked up by different agents simultaneously.

## 0. Parallel waves

```
Wave 0:  A1 A2 A3
Wave 1:  B1..B12
Wave 2:  C1 C2 | D1 D2 D3 D4 D5 | E1 E2 E3 E4 E5
Wave 3:  F1 F2 F3 F4 F5 | R1 R2 R3 R4 (R* can start any time after A)
Wave 4:  G1 G2 G3 G4 | H1 H2 H3 H4 H5 H6 | I1..I6 | L1 L2 L3 L4 L5
Wave 5:  J1..J6 | K1 K2 K3 | M1 M2 | N1 N2 | O1 O2 | P1 P2 P3
Wave 6:  Q1 Q2 Q3 Q4 Q5
```

Within a wave, every package is independent. An agent should prefer packages on
the critical path (anything ending in F, G, J) when unsure.

---

## Wave 0 — Base infrastructure

### A1. Gradle skeleton + modules
- **Deps**: none.
- **Deliverable**: `android/` Gradle project (Kotlin DSL) with modules `app`, `protocol`, `network`, `decode`, `session`, `ui`. Min SDK 26, target latest, Compose enabled.
- **Acceptance**: `./gradlew :app:assembleDebug` succeeds on clean checkout.

### A2. CI job + git hygiene
- **Deps**: A1.
- **Deliverable**: GitHub Actions Android build job in `.github/workflows/ci.yml`; `.gitignore` for `.gradle/`, `build/`.
- **Acceptance**: CI green on PR.

### A3. README + docs
- **Deps**: A1.
- **Deliverable**: `android/README.md` with build/run instructions; link from root `README.md`.
- **Acceptance**: a new contributor can build from README alone.

---

## Wave 1 — Protocol port (one package per Swift file group)

Each B* ports a slice of `Sources/StreamProtocol/` to Kotlin in `android/protocol/`,
with `encode()`/`decode()` and KDoc citing the Swift source. Each has round-trip
unit tests.

### B1. StreamMessage envelope
- **Deps**: A1.
- **Port**: `StreamMessage.swift` (kind, timestamp, isKeyframe, payload).
- **Acceptance**: encode/decode round-trip test.

### B2. Input, Gesture, ScrollDirection
- **Deps**: A1.
- **Port**: `Input.swift`, `Gesture.swift`, `ScrollDirection.swift`.
- **Acceptance**: round-trip tests including all input kinds.

### B3. Clipboard + ClipboardPolicy
- **Deps**: A1.
- **Port**: `Clipboard.swift`, `ClipboardPolicy.swift` (256 KB cap, text only).
- **Acceptance**: cap enforced; round-trip tests.

### B4. MacMenu
- **Deps**: A1.
- **Port**: `MacMenu.swift`.
- **Acceptance**: round-trip tests.

### B5. Viewport + TextScale
- **Deps**: A1.
- **Port**: `Viewport.swift`, `TextScale.swift`.
- **Acceptance**: round-trip tests.

### B6. Pairing
- **Deps**: A1.
- **Port**: `Pairing.swift`.
- **Acceptance**: round-trip tests.

### B7. Remote + RemoteIdentity + RemoteTLS
- **Deps**: A1.
- **Port**: `Remote.swift`, `RemoteIdentity.swift`, `RemoteTLS.swift`.
- **Acceptance**: round-trip + TLS helper tests.

### B8. Audio
- **Deps**: A1.
- **Port**: `Audio.swift`.
- **Acceptance**: round-trip tests.

### B9. Compatibility
- **Deps**: A1.
- **Port**: `Compatibility.swift` (version negotiation, `hostVersion`, `launchID`).
- **Acceptance**: round-trip tests.

### B10. Switcher + Pointer
- **Deps**: A1.
- **Port**: `Switcher.swift`, `Pointer.swift`.
- **Acceptance**: round-trip tests.

### B11. AddressParser + SafeText
- **Deps**: A1.
- **Port**: `AddressParser.swift`, `SafeText.swift`.
- **Acceptance**: round-trip + parsing tests.

### B12. HostSettings
- **Deps**: A1.
- **Port**: `HostSettings.swift` (kinds 16/17).
- **Acceptance**: round-trip tests.

---

## Wave 2 — Tests, discovery, network

### C1. Swift golden emitter
- **Deps**: A1.
- **Deliverable**: small Swift tool (or `SillHost` flag) writing golden byte fixtures for every message kind to `Tests/fixtures/protocol/*.bin` + manifest JSON.
- **Acceptance**: fixtures generated reproducibly.

### C2. Kotlin golden loader
- **Deps**: B1–B12, C1.
- **Deliverable**: Kotlin tests loading each fixture, decoding, re-encoding, comparing bytes; CI runs both.
- **Acceptance**: all fixtures round-trip byte-identical; a broken field fails.

### D1. NSD browser core
- **Deps**: A1.
- **Deliverable**: `network/discovery/` with `NsdManager`-based browser for `_sill._tcp.`.
- **Acceptance**: lists a service on emulator with a test advertiser.

### D2. TXT parsing + HomeDoorTXT
- **Deps**: B11, D1.
- **Deliverable**: parse TXT record (`r`, `p`) via ported logic.
- **Acceptance**: unit tests for all door modes.

### D3. FoundMac + DiscoveryPolicy
- **Deps**: B1–B12, D1, D2.
- **Deliverable**: `FoundMac` data class; port `DiscoveryPolicy` (rows, sightings, direct-wait, browser rebuild on foreground).
- **Acceptance**: unit tests ported from `Tests/checks/home-device`, `home-model`, `home-records`.

### D4. SavedMacs store
- **Deps**: A1.
- **Deliverable**: `SavedMacs` persistence in `DataStore` + `EncryptedSharedPreferences` for keys.
- **Acceptance**: save/load round-trip; encryption verified.

### D5. Browser rebuild on foreground
- **Deps**: D1, D3.
- **Deliverable**: `LifecycleObserver` rebuilds browsers on foreground return (issue #69 behavior).
- **Acceptance**: instrumented test: background → foreground rebuilds.

### E1. Plain TCP socket
- **Deps**: A1.
- **Deliverable**: `network/socket/` plain TCP client with connect/disconnect.
- **Acceptance**: connects to `SillHost` dev mode.

### E2. TLS socket + pinning
- **Deps**: E1, B7.
- **Deliverable**: TLS 1.3 via `SSLSocketFactory` with pinned-key trust manager.
- **Acceptance**: connects to `--pairing` host; wrong key fails.

### E3. Message framer
- **Deps**: B1, E1.
- **Deliverable**: length-prefixed framer matching `StreamMessage` wire layout.
- **Acceptance**: unit tests with golden bytes.

### E4. Connection state machine
- **Deps**: E1, E2, E3.
- **Deliverable**: connect/ready/disconnect/reconnect state machine.
- **Acceptance**: unit tests for transitions.

### E5. RemoteDialPolicy
- **Deps**: B11, E4.
- **Deliverable**: port `RemoteDialPolicy` (candidate selection, VPN detection via `ConnectivityManager`).
- **Acceptance**: unit tests ported from `Tests/checks/remote-rules`.

---

## Wave 3 — Session state

### F1. StreamSession skeleton
- **Deps**: B1–B12, E4.
- **Deliverable**: `session/StreamSession` ViewModel + `StateFlow` holding status, connected, active source.
- **Acceptance**: unit tests for basic transitions.

### F2. Catalog state
- **Deps**: F1.
- **Deliverable**: windows, thumbnails, icons, apps state + handlers for catalog messages.
- **Acceptance**: fake socket drives catalog → state populated.

### F3. Settings state
- **Deps**: F1, B12.
- **Deliverable**: host settings ledger (kinds 16/17) state + picks.
- **Acceptance**: unit tests for settings transitions.

### F4. Menus state
- **Deps**: F1, B4.
- **Deliverable**: menus state (fetch/choice/timeout).
- **Acceptance**: unit tests.

### F5. Reconnect / move logic
- **Deps**: F1, D3, E4.
- **Deliverable**: reconnect, `moveToNetworkIfListed`, path following.
- **Acceptance**: unit tests ported from discovery policy.

### R1. Release signing
- **Deps**: A1.
- **Deliverable**: release signing config in Gradle.
- **Acceptance**: signed build locally.

### R2. ProGuard/R8 rules
- **Deps**: A1.
- **Deliverable**: `proguard-rules.pro` for protocol + Compose.
- **Acceptance**: release build minified runs.

### R3. Play Store metadata
- **Deps**: A1.
- **Deliverable**: adapted metadata from `docs/app-store-metadata.md`.
- **Acceptance**: metadata reviewed.

### R4. CI instrumented tests
- **Deps**: A2.
- **Deliverable**: emulator-based instrumented test job.
- **Acceptance**: CI runs them.

---

## Wave 4 — Decode, input, connect UI, pairing

### G1. MediaCodec HEVC setup
- **Deps**: E3, F1.
- **Deliverable**: `decode/HevcDecoder` using `MediaCodec` (`video/hevc`), output to `Surface`.
- **Acceptance**: decodes a test HEVC stream to Surface.

### G2. Parameter sets handling
- **Deps**: G1.
- **Deliverable**: handle out-of-band parameter sets; feed codec config.
- **Acceptance**: stream starts without waiting for IDR from device.

### G3. Frame dropping + latency
- **Deps**: G1.
- **Deliverable**: keep-latest drop; latency budget ~60 ms.
- **Acceptance**: measured frame age ≤ 60 ms on device.

### G4. Software decode fallback
- **Deps**: G1.
- **Deliverable**: fallback when hardware decoder unavailable.
- **Acceptance**: runs on emulator (software decoder).

### H1. Touch input
- **Deps**: B2, F1.
- **Deliverable**: touch → normalized fractions → `.pointer` messages.
- **Acceptance**: tap lands on Mac.

### H2. Scroll
- **Deps**: B2, F1.
- **Deliverable**: scroll with momentum → `.scroll`.
- **Acceptance**: scroll works.

### H3. Hardware keyboard
- **Deps**: B2, F1.
- **Deliverable**: hardware key → `.key`/typed text; mirror `HardwareKeyboardPolicy`.
- **Acceptance**: Latin keys land; unit tests for policy.

### H4. Soft keyboard / IME
- **Deps**: B2, F1.
- **Deliverable**: soft keyboard → `insertText` (device-side composition for CJK).
- **Acceptance**: CJK input works.

### H5. Gestures
- **Deps**: B2, F1.
- **Deliverable**: 3-finger, pinch → gesture messages.
- **Acceptance**: Mission Control/Exposé trigger.

### H6. Stylus
- **Deps**: B2, F1.
- **Deliverable**: Android stylus → pointer move + pressure.
- **Acceptance**: stylus moves Mac pointer.

### I1. Connect screen scaffold
- **Deps**: D1, F1.
- **Deliverable**: Compose screen skeleton + navigation.
- **Acceptance**: navigates from launcher.

### I2. Found Macs list
- **Deps**: I1, D3.
- **Deliverable**: tiles for found Macs.
- **Acceptance**: shows discovered Macs.

### I3. Saved Macs list + previews
- **Deps**: I1, D4.
- **Deliverable**: saved Macs with previews.
- **Acceptance**: shows saved Macs + thumbnails.

### I4. Add a Mac card
- **Deps**: I1.
- **Deliverable**: Add a Mac card UI.
- **Acceptance**: card renders and navigates to pairing.

### I5. Search nearby + status lines
- **Deps**: I1, D1.
- **Deliverable**: Search Nearby button + status lines + Local Network hint.
- **Acceptance**: toggles nearby browser.

### I6. Local network permission flow
- **Deps**: I1.
- **Deliverable**: permission request + settings deep-link.
- **Acceptance**: permission granted → discovery works.

### L1. QR scan
- **Deps**: I4.
- **Deliverable**: ML Kit Barcode scanner for pairing QR.
- **Acceptance**: scans a Mac's QR and extracts code.

### L2. Code entry
- **Deps**: I4.
- **Deliverable**: 12-digit code entry UI.
- **Acceptance**: accepts code and submits.

### L3. TLS key storage (Keystore)
- **Deps**: E2.
- **Deliverable**: store Mac's pinned identity key in Android Keystore.
- **Acceptance**: key persists across restarts; wrong key fails.

### L4. SavedMac persistence + groups
- **Deps**: D4, L3.
- **Deliverable**: persist saved Macs + groups + previews cache.
- **Acceptance**: survives app restart.

### L5. Remove / forget flows
- **Deps**: L4.
- **Deliverable**: remove/forget Mac UI + logic.
- **Acceptance**: removed Mac gone from list and storage.

---

## Wave 5 — Stream UI + features

### J1. Stream screen scaffold + video surface
- **Deps**: F1, G1, I1.
- **Deliverable**: stream screen with video surface.
- **Acceptance**: shows stream after pick.

### J2. Window bar + reorder
- **Deps**: J1, F2.
- **Deliverable**: thumbnails bar, drag-to-reorder persisted per Mac.
- **Acceptance**: reorder persists.

### J3. App drawer
- **Deps**: J1, F2.
- **Deliverable**: searchable installed-apps list + launch.
- **Acceptance**: search + launch works.

### J4. Settings panel
- **Deps**: J1, F3.
- **Deliverable**: quality/resolution/fps, Aa, reverse scroll, sound mute, clipboard toggles.
- **Acceptance**: settings changes reach host.

### J5. Menus button + trackpad area
- **Deps**: J1, F4, H1.
- **Deliverable**: menus button + portrait trackpad area.
- **Acceptance**: trackpad moves pointer; menus opens.

### J6. Pointer sprite overlay
- **Deps**: J1.
- **Deliverable**: overlay showing Mac's pointer (kind 26).
- **Acceptance**: pointer visible and tracks.

### K1. Viewport reporting
- **Deps**: F1, J1.
- **Deliverable**: report panel size + text scale to host.
- **Acceptance**: host receives viewport.

### K2. Aa slider
- **Deps**: K1.
- **Deliverable**: Aa slider with Off semantics (restore on Off).
- **Acceptance**: text size changes; Off restores.

### K3. Thumbnail hold actions
- **Deps**: J2.
- **Deliverable**: long-press thumbnail: close/minimize/fullscreen.
- **Acceptance**: actions work.

### M1. Clipboard Mac → Android
- **Deps**: F1, E3.
- **Deliverable**: host clipboard (kind 32) → Android clipboard.
- **Acceptance**: copy on Mac → paste on Android.

### M2. Clipboard Android → Mac
- **Deps**: F1, E3.
- **Deliverable**: Android clipboard change → send text (≤ 256 KB).
- **Acceptance**: copy on Android → paste on Mac.

### N1. Menu fetch + render
- **Deps**: F4, E3.
- **Deliverable**: fetch menu tree (kinds 24/25), render in Compose.
- **Acceptance**: menu tree displays.

### N2. Menu choice + timeout
- **Deps**: N1.
- **Deliverable**: choice (kind 27) with token; 4 s RTT timeout; "didn't answer" UI.
- **Acceptance**: item activates; timeout surfaces.

### O1. Audio decode
- **Deps**: F1, G1.
- **Deliverable**: decode audio packets (kind 43) via `MediaCodec`/`AudioTrack`.
- **Acceptance**: audio plays.

### O2. Audio sync + mute
- **Deps**: O1.
- **Deliverable**: sync to video (slightly behind); mute switch persisted; Send Audio toggle.
- **Acceptance**: sync within ~100 ms; mute works.

### P1. Candidate dialing
- **Deps**: F1, L3, E5.
- **Deliverable**: dial saved addresses; VPN detection.
- **Acceptance**: connects over VPN.

### P2. Move home
- **Deps**: P1, D3.
- **Deliverable**: move session from remote door to home door when network lists Mac.
- **Acceptance**: auto-moves home on LAN return.

### P3. Away quality + network changes
- **Deps**: P1.
- **Deliverable**: away quality when remote; home quality when local; `ConnectivityManager` handling.
- **Acceptance**: quality switches with route.

---

## Wave 6 — Polish

### Q1. TourPolicy port
- **Deps**: A1.
- **Deliverable**: port `TourPolicy` to Kotlin.
- **Acceptance**: unit tests.

### Q2. Tour overlay
- **Deps**: Q1, J1.
- **Deliverable**: first-run walkthrough Compose overlay; "New:" rows; Take the Tour.
- **Acceptance**: tour shows once; re-runnable.

### Q3. Phone / tablet layouts
- **Deps**: J1.
- **Deliverable**: responsive layouts for phone vs tablet, portrait/landscape.
- **Acceptance**: usable on both form factors.

### Q4. Accessibility (TalkBack)
- **Deps**: J1.
- **Deliverable**: TalkBack labels, large text support.
- **Acceptance**: TalkBack navigable.

### Q5. Diagnostics HUD
- **Deps**: J1.
- **Deliverable**: debug HUD (link stats, fps, frame age).
- **Acceptance**: toggled in debug builds.

---

## Cross-cutting rules

1. No heavy third-party deps beyond AndroidX + ML Kit (QR). Prefer AndroidX/MediaCodec/NSD.
2. Port policy logic as pure functions with unit tests, mirroring `Tests/checks/`. Keep out of UI.
3. Golden tests gate protocol changes.
4. Match iOS behavior unless a package note says otherwise; document divergences.
5. Each package = one PR with its own tests.

## First concrete steps

1. A1 → A2/A3 (Wave 0).
2. B1–B12 in parallel (Wave 1).
3. C1 + D1 + E1 in parallel (Wave 2 start).
4. Then F1, and so on.
