//
//  PerformanceMetric.swift
//  ClawPhones
//
//  Performance and health monitoring data models
//

import Foundation

// MARK: - App Performance Metrics
struct AppMetrics: Codable {
    /// Time taken for app to launch and become interactive (milliseconds)
    let startupTimeMs: Double

    /// Current memory usage (megabytes)
    let memoryUsageMB: Double

    /// Current CPU usage percentage (0-100)
    let cpuUsagePercent: Double

    /// Estimated battery drain per hour (percentage)
    let batteryDrainPerHour: Double

    /// Total bytes received since last report
    let networkBytesIn: Int64

    /// Total bytes sent since last report
    let networkBytesOut: Int64

    /// Number of active network connections
    let activeConnections: Int

    /// Number of dropped frames since last measurement
    let frameDropCount: Int

    /// Cache hit rate as percentage (0-100)
    let cacheHitRate: Double

    /// Timestamp when metrics were collected
    let collectedAt: Date

    init(
        startupTimeMs: Double = 0,
        memoryUsageMB: Double,
        cpuUsagePercent: Double,
        batteryDrainPerHour: Double = 0,
        networkBytesIn: Int64 = 0,
        networkBytesOut: Int64 = 0,
        activeConnections: Int = 0,
        frameDropCount: Int = 0,
        cacheHitRate: Double = 0,
        collectedAt: Date = Date()
    ) {
        self.startupTimeMs = startupTimeMs
        self.memoryUsageMB = memoryUsageMB
        self.cpuUsagePercent = cpuUsagePercent
        self.batteryDrainPerHour = batteryDrainPerHour
        self.networkBytesIn = networkBytesIn
        self.networkBytesOut = networkBytesOut
        self.activeConnections = activeConnections
        self.frameDropCount = frameDropCount
        self.cacheHitRate = cacheHitRate
        self.collectedAt = collectedAt
    }
}

// MARK: - Sync Status
struct SyncStatus: Codable {
    /// Timestamp of last successful sync
    let lastSyncAt: Date?

    /// Number of items pending upload
    let pendingUploads: Int

    /// Number of items pending download
    let pendingDownloads: Int

    /// Number of conflicts that need resolution
    let conflictCount: Int

    /// Overall sync health status
    let syncHealthy: Bool

    /// Current sync state
    let syncState: SyncState

    /// Error message if sync failed
    let lastError: String?

    enum SyncState: String, Codable {
        case idle
        case syncing
        case paused
        case error
    }

    init(
        lastSyncAt: Date? = nil,
        pendingUploads: Int = 0,
        pendingDownloads: Int = 0,
        conflictCount: Int = 0,
        syncHealthy: Bool = true,
        syncState: SyncState = .idle,
        lastError: String? = nil
    ) {
        self.lastSyncAt = lastSyncAt
        self.pendingUploads = pendingUploads
        self.pendingDownloads = pendingDownloads
        self.conflictCount = conflictCount
        self.syncHealthy = syncHealthy
        self.syncState = syncState
        self.lastError = lastError
    }
}

// MARK: - Health Check Results
struct HealthCheck: Codable {
    /// Relay service reachable
    let relayReachable: Bool

    /// Backend API reachable
    let backendReachable: Bool

    /// WebSocket connected
    let wsConnected: Bool

    /// Push notifications registered
    let pushRegistered: Bool

    /// Average latency to backend (milliseconds)
    let latencyMs: Double

    /// When health check was performed
    let lastCheckedAt: Date

    /// Relay specific metrics
    let relayLatencyMs: Double?

    /// Backend specific metrics
    let backendLatencyMs: Double?

    /// Overall health status
    let overallHealth: HealthStatus

    enum HealthStatus: String, Codable {
        case healthy
        case degraded
        case unhealthy
    }

    init(
        relayReachable: Bool,
        backendReachable: Bool,
        wsConnected: Bool,
        pushRegistered: Bool,
        latencyMs: Double,
        lastCheckedAt: Date = Date(),
        relayLatencyMs: Double? = nil,
        backendLatencyMs: Double? = nil
    ) {
        self.relayReachable = relayReachable
        self.backendReachable = backendReachable
        self.wsConnected = wsConnected
        self.pushRegistered = pushRegistered
        self.latencyMs = latencyMs
        self.lastCheckedAt = lastCheckedAt
        self.relayLatencyMs = relayLatencyMs
        self.backendLatencyMs = backendLatencyMs

        // Determine overall health based on individual components
        if relayReachable && backendReachable && wsConnected && latencyMs < 500 {
            self.overallHealth = .healthy
        } else if relayReachable || backendReachable {
            self.overallHealth = .degraded
        } else {
            self.overallHealth = .unhealthy
        }
    }
}
