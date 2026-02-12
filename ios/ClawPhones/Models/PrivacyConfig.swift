//
//  PrivacyConfig.swift
//  ClawPhones
//

import Foundation

// MARK: - Privacy Settings

/// Represents user privacy settings for the app
struct PrivacySettings: Codable {
    /// Whether face blurring is enabled for uploaded images
    var faceBlurEnabled: Bool

    /// Whether license plate blurring is enabled for uploaded images
    var licensePlateBlurEnabled: Bool

    /// Precision level for location data sharing
    var locationPrecision: LocationPrecision

    /// Number of days to retain user data before automatic deletion
    var dataRetentionDays: Int

    /// Whether to share anonymous analytics with the app developers
    var shareAnalytics: Bool

    /// Whether to share anonymized data with the community for safety alerts
    var shareWithCommunity: Bool

    /// Whether to enable end-to-end encryption for data storage
    var encryptionEnabled: Bool

    /// Number of days after which sensitive data is automatically deleted
    var autoDeleteAfterDays: Int

    /// Default privacy settings with reasonable defaults
    static let `default` = PrivacySettings(
        faceBlurEnabled: true,
        licensePlateBlurEnabled: true,
        locationPrecision: .neighborhood,
        dataRetentionDays: 365,
        shareAnalytics: false,
        shareWithCommunity: true,
        encryptionEnabled: true,
        autoDeleteAfterDays: 30
    )

    enum LocationPrecision: String, Codable, CaseIterable {
        case exact
        case neighborhood
        case city
        case disabled

        var displayName: String {
            switch self {
            case .exact: return "Exact Location"
            case .neighborhood: return "Neighborhood"
            case .city: return "City Level"
            case .disabled: return "Disabled"
            }
        }

        var description: String {
            switch self {
            case .exact: return "Share precise GPS coordinates"
            case .neighborhood: return "Share approximate location within neighborhood"
            case .city: return "Share only city-level location"
            case .disabled: return "Do not share any location data"
            }
        }
    }
}

// MARK: - Data Export Request

/// Represents a request to export user data
struct DataExportRequest: Codable, Identifiable {
    /// Unique identifier for the export request
    var id: String

    /// Current status of the export request
    var status: ExportStatus

    /// When the export was requested
    var requestedAt: Date

    /// When the export was completed (if applicable)
    var completedAt: Date?

    /// URL to download the exported data (when ready)
    var downloadURL: URL?

    /// When the download link expires
    var expiresAt: Date?

    /// Format of the exported data
    var format: ExportFormat

    enum ExportStatus: String, Codable {
        case pending
        case processing
        case ready
        case expired

        var displayName: String {
            switch self {
            case .pending: return "Pending"
            case .processing: return "Processing"
            case .ready: return "Ready"
            case .expired: return "Expired"
            }
        }

        var canDownload: Bool {
            self == .ready
        }

        var isComplete: Bool {
            self == .ready || self == .expired
        }

        var isActive: Bool {
            self == .pending || self == .processing
        }
    }

    enum ExportFormat: String, Codable, CaseIterable {
        case json
        case csv

        var displayName: String {
            switch self {
            case .json: return "JSON"
            case .csv: return "CSV"
            }
        }

        var fileExtension: String {
            switch self {
            case .json: return "json"
            case .csv: return "csv"
            }
        }
    }
}

// MARK: - Privacy Audit Log

/// Represents an entry in the privacy audit log
struct PrivacyAuditLog: Codable, Identifiable {
    /// Unique identifier for the log entry
    var id: String

    /// The action that was performed
    var action: AuditAction

    /// Type of data affected by the action
    var dataType: String

    /// When the action occurred
    var timestamp: Date

    /// Additional details about the action
    var details: String?

    enum AuditAction: String, Codable {
        case dataAccessed
        case dataExported
        case dataDeleted
        case settingsChanged
        case consentGranted
        case consentRevoked
        case privacyViolationReported
        case encryptionKeyRotated

        var displayName: String {
            switch self {
            case .dataAccessed: return "Data Accessed"
            case .dataExported: return "Data Exported"
            case .dataDeleted: return "Data Deleted"
            case .settingsChanged: return "Settings Changed"
            case .consentGranted: return "Consent Granted"
            case .consentRevoked: return "Consent Revoked"
            case .privacyViolationReported: return "Privacy Violation Reported"
            case .encryptionKeyRotated: return "Encryption Key Rotated"
            }
        }
    }
}

// MARK: - Consent Record

/// Represents a user's consent for a specific feature or data usage
struct ConsentRecord: Codable {
    /// Name/identifier of the feature or data type
    var featureName: String

    /// Whether consent has been granted
    var granted: Bool

    /// When consent was granted (if applicable)
    var grantedAt: Date?

    /// When consent was revoked (if applicable)
    var revokedAt: Date?

    /// Display name for the feature
    var displayName: String

    /// Description of what this consent allows
    var description: String

    /// Whether this consent is required for app functionality
    var isRequired: Bool

    var isActive: Bool {
        granted && revokedAt == nil
    }

    static let cameraConsent = ConsentRecord(
        featureName: "camera",
        granted: false,
        grantedAt: nil,
        revokedAt: nil,
        displayName: "Camera Access",
        description: "Allow the app to capture photos and videos for community alerts",
        isRequired: true
    )

    static let locationConsent = ConsentRecord(
        featureName: "location",
        granted: false,
        grantedAt: nil,
        revokedAt: nil,
        displayName: "Location Access",
        description: "Allow the app to access your location for neighborhood safety",
        isRequired: true
    )

    static let notificationsConsent = ConsentRecord(
        featureName: "notifications",
        granted: false,
        grantedAt: nil,
        revokedAt: nil,
        displayName: "Push Notifications",
        description: "Receive alerts about safety incidents in your community",
        isRequired: false
    )

    static let analyticsConsent = ConsentRecord(
        featureName: "analytics",
        granted: false,
        grantedAt: nil,
        revokedAt: nil,
        displayName: "Anonymous Analytics",
        description: "Share anonymous usage data to help improve the app",
        isRequired: false
    )
}
