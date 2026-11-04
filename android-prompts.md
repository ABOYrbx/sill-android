# Prompts for multi-agent implementation of Sill for Android

Copy-paste these prompts to subagents. Run them in the waves indicated so
dependencies are satisfied. The Mac side (`Sources/`) is read-only reference;
all new code goes under `android/`.

Reply language: match the user (German OK).

---

## Prompt 0 — Explore the project (run first, once)

```
You are onboarding to the Sill repository. Goal: build an accurate mental model
before any implementation. Do not write code.

Tasks:
1. Read README.md, CLAUDE.md (first ~200 lines + the "Layout" section), docs/BRIEF.md,
   docs/DEVELOPMENT.md (build/test sections), and AGENTS.md.
2. Walk the tree: Package.swift, Sources/StreamProtocol/*, Sources/SillHost/StreamCoordinator.swift,
   Sources/SillHostCLI/main.swift, Sources/SillMenuBar/main.swift, iOSClient/StreamClient.swift (top 200 lines).
3. Summarize in <400 words:
   - What Sill is and the two sides.
   - The shared wire protocol (StreamMessage kinds) and where it lives.
   - The host engine's main flow (capture→encode→network; input back).
   - The device side's main flow (discovery→connect→catalog→pick→stream→input).
   - Build/test commands.
   - Conventions: pure policy functions + mutants, no third-party deps, main-actor host.
4. List the 5 files you'd read first to start a new feature, with one-line reasons.

Deliverable: a short orientation note (markdown). No code changes.
```

---

## Wave 0 — Base infrastructure

### Prompt A1 — Gradle skeleton + modules

```
Context: Sill repo. AGENTS.md says commits go only to ABOYrbx/sill-android origin,
author "ABOYrbx <ABOYrbx@users.noreply.github.com>". Read AGENTS.md first.

Task: scaffold the Android project under android/.

Deliverables:
- android/ Gradle project (Kotlin DSL) with modules: app, protocol, network, decode, session, ui.
- Min SDK 26, target latest, Jetpack Compose enabled in app/ui.
- android/README.md with build/run instructions.
- Empty but compilable module sources.

Acceptance: `./gradlew :app:assembleDebug` succeeds on clean checkout.
Do not add heavy third-party deps. Commit when done.
```

### Prompt A2 — CI job + git hygiene

```
Context: A1 done. Read AGENTS.md.

Task:
- Add a GitHub Actions Android build job to .github/workflows/ci.yml (debug assemble).
- Add .gitignore entries for .gradle/, build/, *.iml.

Acceptance: CI green on PR. Commit.
```

### Prompt A3 — README link

```
Context: A1 done.

Task: add a short "Android client" section to README.md pointing to android/README.md,
and link android/README.md from root README.

Acceptance: docs consistent. Commit.
```

---

## Wave 1 — Protocol port (one agent per B* package; all independent)

Common instruction for all B* prompts:
"Port the named Swift file(s) from Sources/StreamProtocol/ to Kotlin in android/protocol/,
mirroring types and field order exactly. Add encode()/decode() matching the Swift wire layout.
Add round-trip unit tests in protocol/src/test. Cite the Swift source file in KDoc.
No third-party deps. Commit."

### Prompt B1 — StreamMessage envelope
Port `StreamMessage.swift` (kind, timestamp, isKeyframe, payload). Round-trip tests.

### Prompt B2 — Input, Gesture, ScrollDirection
Port `Input.swift`, `Gesture.swift`, `ScrollDirection.swift`. All input kinds covered in tests.

### Prompt B3 — Clipboard + ClipboardPolicy
Port `Clipboard.swift`, `ClipboardPolicy.swift` (256 KB cap, text only). Cap enforced in tests.

### Prompt B4 — MacMenu
Port `MacMenu.swift`. Round-trip tests.

### Prompt B5 — Viewport + TextScale
Port `Viewport.swift`, `TextScale.swift`. Round-trip tests.

### Prompt B6 — Pairing
Port `Pairing.swift`. Round-trip tests.

### Prompt B7 — Remote + RemoteIdentity + RemoteTLS
Port `Remote.swift`, `RemoteIdentity.swift`, `RemoteTLS.swift`. TLS helper tests.

### Prompt B8 — Audio
Port `Audio.swift`. Round-trip tests.

### Prompt B9 — Compatibility
Port `Compatibility.swift` (version negotiation, hostVersion, launchID). Round-trip tests.

### Prompt B10 — Switcher + Pointer
Port `Switcher.swift`, `Pointer.swift`. Round-trip tests.

### Prompt B11 — AddressParser + SafeText
Port `AddressParser.swift`, `SafeText.swift`. Parsing + sanitization tests.

### Prompt B12 — HostSettings
Port `HostSettings.swift` (kinds 16/17). Round-trip tests.

---

## Wave 2 — Golden tests, discovery, network

### Prompt C1 — Swift golden emitter

```
Context: B* done. Read Sources/StreamProtocol/*.

Task: add a small Swift tool (or a SillHost flag --dump-protocol) that writes golden
byte fixtures for every message kind to Tests/fixtures/protocol/*.bin plus a manifest.json.
Each fixture: one encoded message of that kind.

Acceptance: fixtures generated reproducibly (run twice, diff empty). Commit fixtures.
```

### Prompt C2 — Kotlin golden loader

```
Context: B* and C1 done.

Task: Kotlin tests in android/protocol that load each fixture, decode, re-encode,
compare bytes. Wire into CI.

Acceptance: all fixtures round-trip byte-identical; a deliberately broken field fails.
Commit.
```

### Prompt D1 — NSD browser core

```
Context: read iOSClient/StreamClient.swift discovery section + docs/DEVELOPMENT.md.

Task: implement network/discovery/ with NsdManager-based browser for _sill._tcp.
Deliverable: lists a service on emulator with a test advertiser.

Acceptance: instrumented test passes. Commit.
```

### Prompt D2 — TXT parsing + HomeDoorTXT

```
Context: B11 done.

Task: parse TXT record (r, p tags) via ported logic. Unit tests for all door modes.
Commit.
```

### Prompt D3 — FoundMac + DiscoveryPolicy

```
Context: B*, D1, D2 done. Read iOSClient discovery + Tests/checks/home-device, home-model, home-records.

Task: FoundMac data class; port DiscoveryPolicy (rows, sightings, direct-wait, browser rebuild).
Unit tests ported from the checks above.

Acceptance: tests pass. Commit.
```

### Prompt D4 — SavedMacs store

```
Task: SavedMacs persistence in DataStore + EncryptedSharedPreferences for keys.
Round-trip + encryption tests. Commit.
```

### Prompt D5 — Browser rebuild on foreground

```
Context: D1, D3 done.

Task: LifecycleObserver rebuilds browsers on foreground return. Instrumented test.
Commit.
```

### Prompt E1 — Plain TCP socket

```
Task: network/socket/ plain TCP client with connect/disconnect. Connects to SillHost dev mode.
Commit.
```

### Prompt E2 — TLS socket + pinning

```
Context: E1, B7 done.

Task: TLS 1.3 via SSLSocketFactory with pinned-key trust manager. Connects to --pairing host;
wrong key fails. Commit.
```

### Prompt E3 — Message framer

```
Context: B1, E1 done.

Task: length-prefixed framer matching StreamMessage wire layout. Unit tests with golden bytes.
Commit.
```

### Prompt E4 — Connection state machine

```
Context: E1-E3 done.

Task: connect/ready/disconnect/reconnect state machine. Unit tests. Commit.
```

### Prompt E5 — RemoteDialPolicy

```
Context: B11, E4 done. Read Tests/checks/remote-rules.

Task: port RemoteDialPolicy (candidate selection, VPN detection via ConnectivityManager).
Unit tests ported. Commit.
```

---

## Wave 3 — Session state

### Prompt F1 — StreamSession skeleton

```
Context: B*, E4 done. Read iOSClient/StreamClient.swift top.

Task: session/StreamSession ViewModel + StateFlow holding status, connected, active source.
Unit tests for basic transitions. Commit.
```

### Prompt F2 — Catalog state

```
Context: F1 done.

Task: windows, thumbnails, icons, apps state + handlers for catalog messages.
Fake-socket test drives catalog → state populated. Commit.
```

### Prompt F3 — Settings state

```
Context: F1, B12 done.

Task: host settings ledger (kinds 16/17) state + picks. Unit tests. Commit.
```

### Prompt F4 — Menus state

```
Context: F1, B4 done.

Task: menus state (fetch/choice/timeout). Unit tests. Commit.
```

### Prompt F5 — Reconnect / move logic

```
Context: F1, D3, E4 done.

Task: reconnect, moveToNetworkIfListed, path following. Unit tests from discovery policy. Commit.
```

### Prompt R1 — Release signing

```
Task: release signing config in Gradle. Signed build locally. Commit.
```

### Prompt R2 — ProGuard/R8 rules

```
Task: proguard-rules.pro for protocol + Compose. Release build minified runs. Commit.
```

### Prompt R3 — Play Store metadata

```
Task: adapted metadata from docs/app-store-metadata.md. Commit.
```

### Prompt R4 — CI instrumented tests

```
Context: A2 done.

Task: emulator-based instrumented test job in CI. Commit.
```

---

## Wave 4 — Decode, input, connect UI, pairing

### Prompt G1 — MediaCodec HEVC setup

```
Context: E3, F1 done. Read iOSClient HEVCDisplayView + StreamClient decode.

Task: decode/HevcDecoder using MediaCodec (video/hevc), output to Surface. Commit.
```

### Prompt G2 — Parameter sets handling

```
Context: G1 done.

Task: handle out-of-band parameter sets; feed codec config. Stream starts without waiting for IDR. Commit.
```

### Prompt G3 — Frame dropping + latency

```
Context: G1 done.

Task: keep-latest drop; latency budget ~60ms. Measured frame age ≤60ms. Commit.
```

### Prompt G4 — Software decode fallback

```
Context: G1 done.

Task: fallback when hardware decoder unavailable. Runs on emulator. Commit.
```

### Prompt H1 — Touch input

```
Context: B2, F1 done.

Task: touch → normalized fractions → .pointer messages. Tap lands on Mac. Commit.
```

### Prompt H2 — Scroll

```
Context: B2, F1 done.

Task: scroll with momentum → .scroll. Commit.
```

### Prompt H3 — Hardware keyboard

```
Context: B2, F1 done. Read iOSClient HardwareKeyboardPolicy.

Task: hardware key → .key/typed text; mirror policy. Unit tests. Commit.
```

### Prompt H4 — Soft keyboard / IME

```
Context: B2, F1 done.

Task: soft keyboard → insertText (device-side composition for CJK). CJK input works. Commit.
```

### Prompt H5 — Gestures

```
Context: B2, F1 done.

Task: 3-finger, pinch → gesture messages. Mission Control/Exposé trigger. Commit.
```

### Prompt H6 — Stylus

```
Context: B2, F1 done.

Task: Android stylus → pointer move + pressure. Commit.
```

### Prompt I1 — Connect screen scaffold

```
Context: D1, F1 done.

Task: Compose connect screen skeleton + navigation. Commit.
```

### Prompt I2 — Found Macs list

```
Context: I1, D3 done.

Task: tiles for found Macs. Commit.
```

### Prompt I3 — Saved Macs list + previews

```
Context: I1, D4 done.

Task: saved Macs with previews. Commit.
```

### Prompt I4 — Add a Mac card

```
Context: I1 done.

Task: Add a Mac card UI navigating to pairing. Commit.
```

### Prompt I5 — Search nearby + status lines

```
Context: I1, D1 done.

Task: Search Nearby button + status lines + Local Network hint. Commit.
```

### Prompt I6 — Local network permission flow

```
Context: I1 done.

Task: permission request + settings deep-link. Commit.
```

### Prompt L1 — QR scan

```
Context: I4 done.

Task: ML Kit Barcode scanner for pairing QR. Commit.
```

### Prompt L2 — Code entry

```
Context: I4 done.

Task: 12-digit code entry UI. Commit.
```

### Prompt L3 — TLS key storage (Keystore)

```
Context: E2 done.

Task: store Mac's pinned identity key in Android Keystore. Persists; wrong key fails. Commit.
```

### Prompt L4 — SavedMac persistence + groups

```
Context: D4, L3 done.

Task: persist saved Macs + groups + previews cache. Survives restart. Commit.
```

### Prompt L5 — Remove / forget flows

```
Context: L4 done.

Task: remove/forget Mac UI + logic. Commit.
```

---

## Wave 5 — Stream UI + features

### Prompt J1 — Stream screen scaffold + video surface

```
Context: F1, G1, I1 done.

Task: stream screen with video surface. Shows stream after pick. Commit.
```

### Prompt J2 — Window bar + reorder

```
Context: J1, F2 done.

Task: thumbnails bar, drag-to-reorder persisted per Mac. Commit.
```

### Prompt J3 — App drawer

```
Context: J1, F2 done.

Task: searchable installed-apps list + launch. Commit.
```

### Prompt J4 — Settings panel

```
Context: J1, F3 done.

Task: quality/resolution/fps, Aa, reverse scroll, sound mute, clipboard toggles. Commit.
```

### Prompt J5 — Menus button + trackpad area

```
Context: J1, F4, H1 done.

Task: menus button + portrait trackpad area. Commit.
```

### Prompt J6 — Pointer sprite overlay

```
Context: J1 done.

Task: overlay showing Mac's pointer (kind 26). Commit.
```

### Prompt K1 — Viewport reporting

```
Context: F1, J1 done.

Task: report panel size + text scale to host. Commit.
```

### Prompt K2 — Aa slider

```
Context: K1 done.

Task: Aa slider with Off semantics (restore on Off). Commit.
```

### Prompt K3 — Thumbnail hold actions

```
Context: J2 done.

Task: long-press thumbnail: close/minimize/fullscreen. Commit.
```

### Prompt M1 — Clipboard Mac → Android

```
Context: F1, E3 done.

Task: host clipboard (kind 32) → Android clipboard. Commit.
```

### Prompt M2 — Clipboard Android → Mac

```
Context: F1, E3 done.

Task: Android clipboard change → send text (≤256 KB). Commit.
```

### Prompt N1 — Menu fetch + render

```
Context: F4, E3 done.

Task: fetch menu tree (kinds 24/25), render in Compose. Commit.
```

### Prompt N2 — Menu choice + timeout

```
Context: N1 done.

Task: choice (kind 27) with token; 4s RTT timeout; "didn't answer" UI. Commit.
```

### Prompt O1 — Audio decode

```
Context: F1, G1 done.

Task: decode audio packets (kind 43) via MediaCodec/AudioTrack. Commit.
```

### Prompt O2 — Audio sync + mute

```
Context: O1 done.

Task: sync to video (slightly behind); mute switch persisted; Send Audio toggle. Commit.
```

### Prompt P1 — Candidate dialing

```
Context: F1, L3, E5 done.

Task: dial saved addresses; VPN detection. Commit.
```

### Prompt P2 — Move home

```
Context: P1, D3 done.

Task: move session from remote door to home door when network lists Mac. Commit.
```

### Prompt P3 — Away quality + network changes

```
Context: P1 done.

Task: away quality when remote; home quality when local; ConnectivityManager handling. Commit.
```

---

## Wave 6 — Polish

### Prompt Q1 — TourPolicy port

```
Task: port TourPolicy to Kotlin. Unit tests. Commit.
```

### Prompt Q2 — Tour overlay

```
Context: Q1, J1 done.

Task: first-run walkthrough Compose overlay; "New:" rows; Take the Tour. Commit.
```

### Prompt Q3 — Phone / tablet layouts

```
Context: J1 done.

Task: responsive layouts for phone vs tablet, portrait/landscape. Commit.
```

### Prompt Q4 — Accessibility (TalkBack)

```
Context: J1 done.

Task: TalkBack labels, large text support. Commit.
```

### Prompt Q5 — Diagnostics HUD

```
Context: J1 done.

Task: debug HUD (link stats, fps, frame age). Toggled in debug builds. Commit.
```

---

## Orchestration prompt (for the lead agent)

```
You are the lead coordinating the Android port of Sill. The plan is in android.md
and android-prompts.md. Rules:
- Respect the wave order; only start a package when its deps are merged.
- Assign one package per subagent; prefer critical path (F, G, J).
- Each subagent must follow AGENTS.md: commits only to ABOYrbx/sill-android origin,
  author "ABOYrbx <ABOYrbx@users.noreply.github.com>", never touch upstream.
- Each package = one PR with tests. Review before merging.
- Keep the Mac side read-only.
- Report progress per wave.
```
