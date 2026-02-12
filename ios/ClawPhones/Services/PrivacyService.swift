//
//  PrivacyService.swift
//  ClawPhones
//

import Foundation
import Combine

@MainActor
final class PrivacyService: ObservableObject {
    static let shared = PrivacyService()

    // MARK: - Published Properties

    /// Current privacy settings for the user
    @Published var settings: PrivacySettings = PrivacySettings.default

    /// Audit log of privacy-related actions
    @Published var auditLog: [PrivacyAuditLog] = []

    /// Pending data export requests
    @Published var pendingExports: [DataExportRequest] = []

    /// User consent records
    @Published var consents: [ConsentRecord] = []

    /// Loading state indicator
    @Published var isLoading: Bool = false

    /// Last error message
    @Published var errorMessage: String?

    // MARK: - Private Properties

    private let session: URLSession
    private let baseURLDefaultsKey = "ai.clawphones.api.url"
    private let tokenKeychainKey = "relay_token"

    private init(session: URLSession = .shared) {
        self.session = session
        loadConsentsLocally()
    }

    // MARK: - Error Types

    enum PrivacyError: LocalizedError {
        case invalidAPIURL(String)
        case missingToken
        case invalidResponse
        case http(statusCode: Int, message: String)
        case decodeFailed
        case confirmationRequired
        case confirmationInvalid
        case exportNotReady
        case exportExpired

        var errorDescription: String? {
            switch self {
            case .invalidAPIURL(let url):
                return "Invalid API URL: \(url)"
            case .missingToken:
                return "Authentication token is missing."
            case .invalidResponse:
                return "Server returned an invalid response."
            case .http(let statusCode, let message):
                return "HTTP \(statusCode): \(message)"
            case .decodeFailed:
                return "Failed to decode server response."
            case .confirmationRequired:
                return "Confirmation is required for this action."
            case .confirmationInvalid:
                return "Confirmation token is invalid or expired."
            case .exportNotReady:
                return "Export is not ready for download yet."
            case .exportExpired:
                return "Export link has expired."
            }
        }
    }

    // MARK: - Request Builders

    private var apiURL: String {
        let raw = UserDefaults.standard.string(forKey: baseURLDefaultsKey) ?? ""
        return normalizedURL(from: raw) ?? "http://localhost:8787"
    }

    private var authToken: String? {
        KeychainHelper.shared.read(key: tokenKeychainKey)
    }

    private func endpointURL(path: String) throws -> URL {
        guard let base = normalizedURL(from: apiURL),
              let url = URL(string: "\(base)\(path)") else {
            throw PrivacyError.invalidAPIURL(apiURL)
        }
        return url
    }

    private func buildRequest(url: URL, method: String = "GET") throws -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = authToken {
            request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        } else {
            throw PrivacyError.missingToken
        }

        return request
    }

    private func perform(_ request: URLRequest) async throws -> Data {
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw PrivacyError.invalidResponse
        }

        guard (200 ..< 300).contains(http.statusCode) else {
            let message: String
            if let text = String(data: data, encoding: .utf8), !text.isEmpty {
                message = text
            } else {
                message = HTTPURLResponse.localizedString(forStatusCode: http.statusCode)
            }
            throw PrivacyError.http(statusCode: http.statusCode, message: message)
        }

        return data
    }

    private func normalizedURL(from raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        var normalized = trimmed
        while normalized.hasSuffix("/") {
            normalized.removeLast()
        }

        guard let url = URL(string: normalized),
              let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https",
              url.host != nil else {
            return nil
        }

        return normalized
    }

    // MARK: - Settings Management

    /// Fetch current privacy settings from the server
    func fetchSettings() async throws {
        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        let endpoint = try endpointURL(path: "/v1/privacy/settings")
        let request = try buildRequest(url: endpoint, method: "GET")

        let data = try await perform(request)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        settings = try decoder.decode(PrivacySettings.self, from: data)
    }

    /// Update privacy settings
    func updateSettings(_ newSettings: PrivacySettings) async throws {
        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        let endpoint = try endpointURL(path: "/v1/privacy/settings")
        var request = try buildRequest(url: endpoint, method: "PUT")

        struct UpdateSettingsRequest: Codable {
            let faceBlurEnabled: Bool
            let licensePlateBlurEnabled: Bool
            let locationPrecision: String
            let dataRetentionDays: Int
            let shareAnalytics: Bool
            let shareWithCommunity: Bool
            let encryptionEnabled: Bool
            let autoDeleteAfterDays: Int

            enum CodingKeys: String, CodingKey {
                case faceBlurEnabled = "face_blur_enabled"
                case licensePlateBlurEnabled = "license_plate_blur_enabled"
                case locationPrecision = "location_precision"
                case dataRetentionDays = "data_retention_days"
                case shareAnalytics = "share_analytics"
                case shareWithCommunity = "share_with_community"
                case encryptionEnabled = "encryption_enabled"
                case autoDeleteAfterDays = "auto_delete_after_days"
            }
        }

        let body = UpdateSettingsRequest(
            faceBlurEnabled: newSettings.faceBlurEnabled,
            licensePlateBlurEnabled: newSettings.licensePlateBlurEnabled,
            locationPrecision: newSettings.locationPrecision.rawValue,
            dataRetentionDays: newSettings.dataRetentionDays,
            shareAnalytics: newSettings.shareAnalytics,
            shareWithCommunity: newSettings.shareWithCommunity,
            encryptionEnabled: newSettings.encryptionEnabled,
            autoDeleteAfterDays: newSettings.autoDeleteAfterDays
        )

        request.httpBody = try JSONEncoder().encode(body)

        let data = try await perform(request)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        settings = try decoder.decode(PrivacySettings.self, from: data)
    }

    // MARK: - Data Export

    /// Request an export of user data
    func requestDataExport(format: DataExportRequest.ExportFormat) async throws -> DataExportRequest {
        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        let endpoint = try endpointURL(path: "/v1/privacy/export")
        var request = try buildRequest(url: endpoint, method: "POST")

        struct ExportRequest: Codable {
            let format: String
        }

        let body = ExportRequest(format: format.rawValue)
        request.httpBody = try JSONEncoder().encode(body)

        let data = try await perform(request)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let exportRequest = try decoder.decode(DataExportRequest.self, from: data)

        // Add to pending exports if not already present
        if !pendingExports.contains(where: { $0.id == exportRequest.id }) {
            pendingExports.append(exportRequest)
        }

        return exportRequest
    }

    /// Check the status of an export request
    func checkExportStatus(exportId: String) async throws -> DataExportRequest {
        let endpoint = try endpointURL(path: "/v1/privacy/export/\(exportId)")
        let request = try buildRequest(url: endpoint, method: "GET")

        let data = try await perform(request)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let exportRequest = try decoder.decode(DataExportRequest.self, from: data)

        // Update the pending exports list
        if let index = pendingExports.firstIndex(where: { $0.id == exportId }) {
            pendingExports[index] = exportRequest
        } else {
            pendingExports.append(exportRequest)
        }

        // Remove expired exports
        pendingExports.removeAll { $0.status == .expired }

        return exportRequest
    }

    /// Download an exported data file
    func downloadExport(exportRequest: DataExportRequest) async throws -> Data {
        guard exportRequest.status.canDownload else {
            if exportRequest.status == .expired {
                throw PrivacyError.exportExpired
            } else {
                throw PrivacyError.exportNotReady
            }
        }

        guard let downloadURL = exportRequest.downloadURL else {
            throw PrivacyError.invalidResponse
        }

        var request = URLRequest(url: downloadURL)
        request.httpMethod = "GET"

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw PrivacyError.invalidResponse
        }

        guard (200 ..< 300).contains(http.statusCode) else {
            throw PrivacyError.http(
                statusCode: http.statusCode,
                message: HTTPURLResponse.localizedString(forStatusCode: http.statusCode)
            )
        }

        return data
    }

    // MARK: - Data Deletion

    /// Request deletion of all user data with confirmation
    func deleteAllData(confirmation: String) async throws {
        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        let endpoint = try endpointURL(path: "/v1/privacy/data")
        var request = try buildRequest(url: endpoint, method: "DELETE")

        struct DeleteRequest: Codable {
            let confirmation: String
        }

        let body = DeleteRequest(confirmation: confirmation)
        request.httpBody = try JSONEncoder().encode(body)

        _ = try await perform(request)
    }

    // MARK: - Audit Log

    /// Fetch the privacy audit log
    func fetchAuditLog(limit: Int = 100, offset: Int = 0) async throws {
        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        let path = "/v1/privacy/audit?limit=\(limit)&offset=\(offset)"
        let endpoint = try endpointURL(path: path)
        let request = try buildRequest(url: endpoint, method: "GET")

        let data = try await perform(request)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        auditLog = try decoder.decode([PrivacyAuditLog].self, from: data)
    }

    // MARK: - Consent Management

    /// Update consent for a specific feature
    func updateConsent(
        featureName: String,
        granted: Bool
    ) async throws -> ConsentRecord {
        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
        }

        let endpoint = try endpointURL(path: "/v1/privacy/consent")
        var request = try buildRequest(url: endpoint, method: "PUT")

        struct ConsentUpdateRequest: Codable {
            let featureName: String
            let granted: Bool

            enum CodingKeys: String, CodingKey {
                case featureName = "feature_name"
                case granted
            }
        }

        let body = ConsentUpdateRequest(featureName: featureName, granted: granted)
        request.httpBody = try JSONEncoder().encode(body)

        let data = try await perform(request)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let consentRecord = try decoder.decode(ConsentRecord.self, from: data)

        // Update local consents
        if let index = consents.firstIndex(where: { $0.featureName == featureName }) {
            consents[index] = consentRecord
        }

        return consentRecord
    }

    /// Get the current consent status for a feature
    func getConsentStatus(featureName: String) -> ConsentRecord? {
        consents.first { $0.featureName == featureName }
    }

    /// Load consents from local storage
    private func loadConsentsLocally() {
        let defaultsKey = "privacy_consents"
        if let data = UserDefaults.standard.data(forKey: defaultsKey) {
            if let storedConsents = try? JSONDecoder().decode([ConsentRecord].self, from: data) {
                consents = storedConsents
                return
            }
        }

        // Initialize with default consents if none exist
        consents = [
            ConsentRecord.cameraConsent,
            ConsentRecord.locationConsent,
            ConsentRecord.notificationsConsent,
            ConsentRecord.analyticsConsent
        ]
    }

    /// Save consents to local storage
    private func saveConsentsLocally() {
        let defaultsKey = "privacy_consents"
        if let data = try? JSONEncoder().encode(consents) {
            UserDefaults.standard.set(data, forKey: defaultsKey)
        }
    }

    // MARK: - Convenience Methods

    /// Refresh all privacy-related data
    func refreshAll() async throws {
        async let settingsTask: Void = fetchSettings()
        async let auditLogTask: Void = fetchAuditLog()

        try await (settingsTask, auditLogTask)
    }

    /// Clear error message
    func clearError() {
        errorMessage = nil
    }

    /// Check if any pending exports are ready
    var hasReadyExports: Bool {
        pendingExports.contains { $0.status == .ready }
    }

    /// Count of active (pending/processing) exports
    var activeExportCount: Int {
        pendingExports.filter { $0.status.isActive }.count
    }

    /// Reset settings to defaults
    func resetToDefaults() {
        settings = PrivacySettings.default
    }
}
