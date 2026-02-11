//
//  ShareViewController.swift
//  ClawPhonesShare
//

import UIKit
import UniformTypeIdentifiers

private enum ShareExtensionError: LocalizedError {
    case unsupportedContent
    case saveFailed
    case invalidDeepLink

    var errorDescription: String? {
        switch self {
        case .unsupportedContent:
            return "Unsupported shared content"
        case .saveFailed:
            return "Failed to save shared content"
        case .invalidDeepLink:
            return "Failed to open ClawPhones"
        }
    }
}

private enum SharePayloadKind: String, Codable {
    case text
    case url
}

private struct SharedPayload: Codable {
    let id: String
    let content: String
    let kind: SharePayloadKind
    let createdAt: Int

    enum CodingKeys: String, CodingKey {
        case id
        case content
        case kind
        case createdAt = "created_at"
    }
}

final class ShareViewController: UIViewController {
    private static let appGroupID = "group.ai.clawphones.shared"
    private static let latestPayloadIDKey = "ai.clawphones.share.latest_payload_id"
    private static let payloadKeyPrefix = "ai.clawphones.share.payload."

    private let spinner = UIActivityIndicatorView(style: .medium)
    private var hasHandledShare = false

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground
        spinner.translatesAutoresizingMaskIntoConstraints = false
        spinner.hidesWhenStopped = false
        view.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
        spinner.startAnimating()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        guard !hasHandledShare else { return }
        hasHandledShare = true

        Task { [weak self] in
            await self?.handleShare()
        }
    }

    private func handleShare() async {
        do {
            let payload = try await makePayload()
            try persist(payload: payload)
            try openMainApp(payloadID: payload.id)
        } catch {
            extensionContext?.cancelRequest(withError: error)
        }
    }

    private func makePayload() async throws -> SharedPayload {
        guard let items = extensionContext?.inputItems as? [NSExtensionItem], !items.isEmpty else {
            throw ShareExtensionError.unsupportedContent
        }

        if let urlString = try await extractURL(from: items) {
            return SharedPayload(
                id: UUID().uuidString,
                content: urlString,
                kind: .url,
                createdAt: Int(Date().timeIntervalSince1970)
            )
        }

        if let text = try await extractText(from: items) {
            return SharedPayload(
                id: UUID().uuidString,
                content: text,
                kind: .text,
                createdAt: Int(Date().timeIntervalSince1970)
            )
        }

        throw ShareExtensionError.unsupportedContent
    }

    private func extractURL(from items: [NSExtensionItem]) async throws -> String? {
        for item in items {
            guard let attachments = item.attachments else { continue }

            for provider in attachments {
                guard provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) else { continue }
                let loaded = try await loadItem(from: provider, typeIdentifier: UTType.url.identifier)

                if let url = loaded as? URL, !url.absoluteString.isEmpty {
                    return url.absoluteString
                }
                if let url = loaded as? NSURL, let absolute = url.absoluteString, !absolute.isEmpty {
                    return absolute
                }
                if let text = loaded as? String, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    return text.trimmingCharacters(in: .whitespacesAndNewlines)
                }
            }
        }

        return nil
    }

    private func extractText(from items: [NSExtensionItem]) async throws -> String? {
        for item in items {
            if let directText = item.attributedContentText?.string.trimmingCharacters(in: .whitespacesAndNewlines),
               !directText.isEmpty {
                return directText
            }

            guard let attachments = item.attachments else { continue }

            for provider in attachments {
                if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
                    if let text = try await extractText(from: provider, typeIdentifier: UTType.plainText.identifier) {
                        return text
                    }
                }

                if provider.hasItemConformingToTypeIdentifier(UTType.text.identifier) {
                    if let text = try await extractText(from: provider, typeIdentifier: UTType.text.identifier) {
                        return text
                    }
                }
            }
        }

        return nil
    }

    private func extractText(from provider: NSItemProvider, typeIdentifier: String) async throws -> String? {
        let loaded = try await loadItem(from: provider, typeIdentifier: typeIdentifier)

        if let text = loaded as? String {
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? nil : trimmed
        }

        if let attributed = loaded as? NSAttributedString {
            let trimmed = attributed.string.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? nil : trimmed
        }

        return nil
    }

    private func loadItem(from provider: NSItemProvider, typeIdentifier: String) async throws -> NSSecureCoding {
        try await withCheckedThrowingContinuation { continuation in
            provider.loadItem(forTypeIdentifier: typeIdentifier, options: nil) { item, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let secureItem = item else {
                    continuation.resume(throwing: ShareExtensionError.unsupportedContent)
                    return
                }

                continuation.resume(returning: secureItem)
            }
        }
    }

    private func persist(payload: SharedPayload) throws {
        guard let defaults = UserDefaults(suiteName: Self.appGroupID) else {
            throw ShareExtensionError.saveFailed
        }

        do {
            let data = try JSONEncoder().encode(payload)
            defaults.set(data, forKey: Self.payloadKey(for: payload.id))
            defaults.set(payload.id, forKey: Self.latestPayloadIDKey)
        } catch {
            throw ShareExtensionError.saveFailed
        }
    }

    private func openMainApp(payloadID: String) throws {
        guard let escaped = payloadID.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "clawphones://share?payload=\(escaped)") else {
            throw ShareExtensionError.invalidDeepLink
        }

        extensionContext?.open(url) { [weak self] _ in
            self?.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
        }
    }

    private static func payloadKey(for id: String) -> String {
        "\(payloadKeyPrefix)\(id)"
    }
}
