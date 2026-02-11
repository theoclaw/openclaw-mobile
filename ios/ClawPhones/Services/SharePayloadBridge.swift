//
//  SharePayloadBridge.swift
//  ClawPhones
//

import Foundation

enum SharedPayloadKind: String, Codable {
    case text
    case url
}

struct SharedPayload: Codable {
    let id: String
    let content: String
    let kind: SharedPayloadKind
    let createdAt: Int

    enum CodingKeys: String, CodingKey {
        case id
        case content
        case kind
        case createdAt = "created_at"
    }
}

enum SharePayloadBridge {
    static let appGroupID = "group.ai.clawphones.shared"
    static let latestPayloadIDKey = "ai.clawphones.share.latest_payload_id"
    private static let payloadKeyPrefix = "ai.clawphones.share.payload."

    static func payloadID(from url: URL) -> String? {
        guard url.scheme == "clawphones" else { return nil }
        guard url.host == "share" else { return nil }

        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let payloadID = components.queryItems?.first(where: { $0.name == "payload" })?.value,
              !payloadID.isEmpty else {
            return nil
        }

        return payloadID
    }

    static func latestPayloadID() -> String? {
        defaults?.string(forKey: latestPayloadIDKey)
    }

    static func payload(id: String) -> SharedPayload? {
        guard let data = defaults?.data(forKey: payloadKey(for: id)) else { return nil }

        do {
            return try JSONDecoder().decode(SharedPayload.self, from: data)
        } catch {
            return nil
        }
    }

    static func clearPayload(id: String) {
        guard let defaults else { return }

        defaults.removeObject(forKey: payloadKey(for: id))
        if defaults.string(forKey: latestPayloadIDKey) == id {
            defaults.removeObject(forKey: latestPayloadIDKey)
        }
    }

    private static var defaults: UserDefaults? {
        UserDefaults(suiteName: appGroupID)
    }

    private static func payloadKey(for id: String) -> String {
        "\(payloadKeyPrefix)\(id)"
    }
}
