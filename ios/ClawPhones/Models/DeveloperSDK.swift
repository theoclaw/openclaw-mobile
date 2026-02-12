//
//  DeveloperSDK.swift
//  ClawPhones
//

import Foundation

// MARK: - API Key Management

struct APIKey: Codable, Identifiable {
    let id: String
    let name: String
    let key: String
    let permissions: [String]
    let rateLimit: Int?
    let createdAt: Date
    let expiresAt: Date?
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case key
        case permissions
        case rateLimit = "rate_limit"
        case createdAt = "created_at"
        case expiresAt = "expires_at"
        case isActive = "is_active"
    }
}

// MARK: - Webhook Configuration

struct WebhookConfig: Codable, Identifiable {
    let id: String
    let url: String
    let events: [String]
    let secret: String
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case url
        case events
        case secret
        case isActive = "is_active"
    }
}

// MARK: - Usage Records

struct UsageRecord: Codable, Identifiable {
    let date: Date
    let endpoint: String
    let count: Int
    let latencyMs: Int

    var id: String {
        "\(endpoint)-\(ISO8601DateFormatter().string(from: date))"
    }

    enum CodingKeys: String, CodingKey {
        case date
        case endpoint
        case count
        case latencyMs = "latency_ms"
    }
}

// MARK: - SDK Plugins

struct SDKPlugin: Codable, Identifiable {
    let id: String
    let name: String
    let version: String
    let description: String
    let author: String
    let downloadURL: String
    let isInstalled: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case version
        case description
        case author
        case downloadURL = "download_url"
        case isInstalled = "is_installed"
    }
}

// MARK: - API Permissions

enum APIPermission: String, Codable, CaseIterable {
    case readAlerts = "read_alerts"
    case writeAlerts = "write_alerts"
    case readNodes = "read_nodes"
    case manageNodes = "manage_nodes"
    case readCommunity = "read_community"
    case writeCommunity = "write_community"
    case readTasks = "read_tasks"
    case manageTasks = "manage_tasks"
    case readEdge = "read_edge"
    case manageEdge = "manage_edge"

    var displayName: String {
        switch self {
        case .readAlerts: return "Read Alerts"
        case .writeAlerts: return "Write Alerts"
        case .readNodes: return "Read Nodes"
        case .manageNodes: return "Manage Nodes"
        case .readCommunity: return "Read Community"
        case .writeCommunity: return "Write Community"
        case .readTasks: return "Read Tasks"
        case .manageTasks: return "Manage Tasks"
        case .readEdge: return "Read Edge"
        case .manageEdge: return "Manage Edge"
        }
    }

    var category: String {
        switch self {
        case .readAlerts, .writeAlerts: return "Alerts"
        case .readNodes, .manageNodes: return "Nodes"
        case .readCommunity, .writeCommunity: return "Community"
        case .readTasks, .manageTasks: return "Tasks"
        case .readEdge, .manageEdge: return "Edge"
        }
    }
}
