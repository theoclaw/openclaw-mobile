//
//  DeviceConfig.swift
//  ClawPhones
//
//  Device token management (MDM + Keychain fallback)
//

import Foundation

class DeviceConfig {
    static let shared = DeviceConfig()
    private init() {}

    // Managed App Configuration (MDM) keys (also used for local overrides in dev builds)
    static let managedDeviceTokenKey = "ai.openclaw.device_token"
    static let managedBaseURLKey = "ai.openclaw.base_url"
    static let managedModeKey = "ai.openclaw.mode"

    /// Get/set device token.
    /// Priority (read): MDM → Keychain → nil
    /// Behavior (write): non-empty → Keychain, nil/empty → clear all stored tokens.
    var deviceToken: String? {
        get {
            // 1. MDM Managed App Configuration (highest priority)
            if let mdmToken = UserDefaults.standard.string(forKey: Self.managedDeviceTokenKey),
               !mdmToken.isEmpty {
                return mdmToken
            }

            // 2. Keychain (factory pre-installed)
            if let keychainToken = KeychainHelper.shared.readDeviceToken(),
               !keychainToken.isEmpty {
                return keychainToken
            }

            // 3. No token available
            return nil
        }
        set {
            let trimmed = newValue?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if trimmed.isEmpty {
                clearTokens()
            } else {
                saveUserToken(trimmed)
            }
        }
    }

    /// OpenClaw API base URL
    var baseURL: String {
        // Allow MDM override (for testing/staging)
        if let mdmURL = UserDefaults.standard.string(forKey: Self.managedBaseURLKey),
           !mdmURL.isEmpty {
            return mdmURL
        }

        return "http://3.142.69.6:8080"
    }

    /// Device mode/tier
    var mode: Mode {
        if let modeString = UserDefaults.standard.string(forKey: Self.managedModeKey) {
            return Mode(rawValue: modeString) ?? .auto
        }
        return .auto
    }

    enum Mode: String {
        case auto
        case deepseek
        case kimi
        case claude
    }

    /// Check if device is provisioned
    var isProvisioned: Bool {
        return deviceToken != nil
    }

    /// Save user-authenticated token (fallback for non-Oyster devices)
    func saveUserToken(_ token: String) {
        // Write to BOTH UserDefaults (highest priority in getter) AND Keychain.
        // This ensures the getter always returns the freshest token.
        UserDefaults.standard.set(token, forKey: Self.managedDeviceTokenKey)
        _ = KeychainHelper.shared.writeDeviceToken(token)
    }

    /// Clear all stored tokens (for testing)
    func clearTokens() {
        KeychainHelper.shared.clearAll()
        UserDefaults.standard.removeObject(forKey: Self.managedDeviceTokenKey)
    }
}
