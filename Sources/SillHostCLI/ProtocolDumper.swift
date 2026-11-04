import Foundation
import CoreGraphics
import StreamProtocol

/// Writes one golden `StreamMessage` per real `StreamMessageKind` to `<dir>/<name>.bin`,
/// plus a `manifest.json` describing them. Used by `SillHost --dump-protocol <dir>`.
enum ProtocolDumper {
    static let timestamp: Double = 1_700_000_000.0

    struct Entry: Codable {
        let kind: Int
        let rawValue: Int
        let name: String
        let filename: String
        let byteLength: Int
    }

    static func dump(to directory: String) {
        let fm = FileManager.default
        try? fm.createDirectory(atPath: directory, withIntermediateDirectories: true)

        var entries: [Entry] = []

        // (kind, name, payload factory). One encoded StreamMessage per kind.
        let items: [(StreamMessageKind, String, () -> Data)] = [
            (.parameterSets, "parameterSets", {
                ParameterSets(nalUnitHeaderLength: 4, sets: [Data([0x01, 0x02]), Data([0x03, 0x04, 0x05])]).encoded()
            }),
            (.frame, "frame", {
                var d = Data([0, 0, 0, 2])
                d.append(contentsOf: [0xAB, 0xCD])
                return d
            }),
            (.windowList, "windowList", {
                let list = WindowList(macName: "Mac", windows: [WindowInfo(id: 1, title: "Window", appName: "App", bundleID: "com.example", width: 800, height: 600)], active: .window(1), launchID: "launch", hostVersion: "1.0", protocol: 1, gestures: 1, clipboard: 1)
                return Wire.encode(list)
            }),
            (.thumbnail, "thumbnail", {
                ImageBlob.encodeThumbnail(windowID: 1, jpeg: Data([0xFF, 0xD8, 0xFF, 0xE0]))
            }),
            (.appIcon, "appIcon", {
                ImageBlob.encodeIcon(bundleID: "com.example.app", png: Data([0x89, 0x50, 0x4E, 0x47]))
            }),
            (.appList, "appList", {
                Wire.encode([AppInfo(name: "App", bundleID: "com.example.app")])
            }),
            (.selectSource, "selectSource", {
                Wire.encode(StreamSource.window(42))
            }),
            (.launchApp, "launchApp", {
                Wire.encode(LaunchApp(bundleID: "com.example.app"))
            }),
            (.input, "input", {
                Wire.encode(InputEvent.pointer(.move, x: 0.5, y: 0.5))
            }),
            (.viewport, "viewport", {
                Wire.encode(Viewport(width: 1024, height: 768, scale: 1.0, localCursor: true, fps: 60))
            }),
            (.ping, "ping", {
                var value = Double(123).bitPattern.bigEndian
                var d = Data()
                withUnsafeBytes(of: &value) { d.append(contentsOf: $0) }
                return d
            }),
            (.pong, "pong", {
                var value = Double(123).bitPattern.bigEndian
                var d = Data()
                withUnsafeBytes(of: &value) { d.append(contentsOf: $0) }
                return d
            }),
            (.clientStats, "clientStats", {
                Wire.encode(ClientStats(fps: 60, frameAgeMs: 16, rttMs: 5, device: "iPad", frameAgeMaxMs: 32, rttMaxMs: 10, audioBehindMs: 0, audioLate: 0))
            }),
            (.tick, "tick", { Data() }),
            (.cursorShape, "cursorShape", {
                CursorShapeBlob.encode(hotspot: CGPoint(x: 1, y: 2), size: CGSize(width: 3, height: 4), png: Data([0x89, 0x50, 0x4E, 0x47]))
            }),
            (.windowCommand, "windowCommand", {
                Wire.encode(WindowCommand(id: 1, action: .close))
            }),
            (.hostSettings, "hostSettings", {
                let settings = StreamSettings(maxFPS: 60, bitrate: 15_000_000, captureScale: 2, prioritizeSpeed: false, virtualDisplay: true, directWireless: true, sendAudio: false)
                let stream = RunningStream(width: 1920, height: 1080, fps: 60, mbps: 15, onVirtualDisplay: false)
                let away = AwayQuality(homeBitrate: 15_000_000, homeCaptureScale: 2, awayBitrate: 8_000_000, awayCaptureScale: 1, thisConnectionAway: false, awayRunning: false)
                let link = LinkReport(state: LinkReport.behind, withheldPerSecond: 3, bitrate: 15_000_000, carriedKbps: 10_000, suggestedBitrate: 8_000_000, suggestedCaptureScale: 1)
                let state = HostSettingsState(settings: settings, persistent: true, virtualDisplayAvailable: true, softwareEncoder: false, stream: stream, answering: 7, away: away, link: link, sessionPaired: true)
                return Wire.encode(state)
            }),
            (.changeSettings, "changeSettings", {
                Wire.encode(HostSettingsChange(token: 1, maxFPS: 120))
            }),
            (.macInfo, "macInfo", {
                let info = MacInfo(v: 1, macID: "0123456789ABCDEFGH", name: "Mac", issuedAt: timestamp, remoteAccess: true, remotePort: 7455, internet: false, addresses: [MacAddress(host: "mac.local", port: nil, kind: MacAddress.lan, via: "Wi-Fi")])
                let signed = SignedMacInfo(info: Wire.encode(info).base64EncodedString(), key: Data(repeating: 4, count: 65).base64EncodedString(), sig: Data(repeating: 0, count: 72).base64EncodedString())
                return Wire.encode(signed)
            }),
            (.pairRequest, "pairRequest", {
                Wire.encode(PairRequest(v: 1, method: "qr", proof: "proof", name: "iPad", model: "iPad14,1", cable: true))
            }),
            (.pairResult, "pairResult", {
                Wire.encode(PairResult(ok: true, proof: "proof", macID: "MACID", name: "Mac", recognitionKey: "key", reason: nil, triesLeft: nil, retryAfter: nil, method: nil, message: nil))
            }),
            (.pairingWanted, "pairingWanted", { Data() }),
            (.goodbye, "goodbye", {
                Wire.encode(Goodbye(reason: "quit"))
            }),
            (.hello, "hello", {
                Wire.encode(Hello(appVersion: "1.0", build: "42", protocol: 1, device: "iPad", audio: ["aac-eld"]))
            }),
            (.macMenu, "macMenu", {
                Wire.encode(MacMenu(version: 1, app: "Safari", bundleID: "com.apple.Safari", menus: [MacMenuItem(id: "1", title: "File", submenu: true)]))
            }),
            (.pressMenuItem, "pressMenuItem", {
                Wire.encode(PressMenuItem(version: 1, id: "1", title: "New", token: 1))
            }),
            (.macPointer, "macPointer", {
                Wire.encode(MacPointer(x: 0.5, y: 0.5, inside: true, seen: 1))
            }),
            (.fetchMenu, "fetchMenu", {
                Wire.encode(FetchMenu(version: 1, id: "1", title: "File", token: 1))
            }),
            (.gesture, "gesture", {
                Wire.encode(TrackpadGesture(gesture: "swipeUp", fingers: 3))
            }),
            (.clipboard, "clipboard", {
                Wire.encode(Clipboard(text: "hello"))
            }),
            (.audio, "audio", {
                let format = AudioFormat(codec: "aac-eld", sampleRate: 48000, channels: 2, framesPerPacket: 480, primingFrames: 240, bitrate: 128000, epoch: 1, cookie: Data([0x01, 0x02]), source: "app", app: "Safari")
                return AudioMessage.format(format).serialized()
            }),
            (.keyframeRequest, "keyframeRequest", { Data() }),
        ]

        for (kind, name, makePayload) in items {
            let payload = makePayload()
            let isKeyframe = (kind == .parameterSets || kind == .frame)
            let msg = StreamMessage(kind: kind, timestamp: timestamp, isKeyframe: isKeyframe, payload: payload)
            let data = msg.serialized()
            let filename = "\(name).bin"
            let url = URL(fileURLWithPath: directory).appendingPathComponent(filename)
            try? data.write(to: url)
            entries.append(Entry(kind: Int(kind.rawValue), rawValue: Int(kind.rawValue), name: name, filename: filename, byteLength: data.count))
        }

        entries.sort { $0.kind < $1.kind }

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        if let manifestData = try? encoder.encode(entries) {
            let manifestURL = URL(fileURLWithPath: directory).appendingPathComponent("manifest.json")
            try? manifestData.write(to: manifestURL)
        }

        print("Wrote \(entries.count) fixtures to \(directory)")
    }
}