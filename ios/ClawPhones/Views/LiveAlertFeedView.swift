//
//  LiveAlertFeedView.swift
//  ClawPhones
//

import SwiftUI

/// Real-time alert feed view with WebSocket integration
struct LiveAlertFeedView: View {
    @StateObject private var webSocketService = WebSocketService()
    @State private var alerts: [LiveAlert] = []
    @State private var selectedType: LiveAlertTypeFilter = .all
    @State private var selectedSeverity: SeverityFilter = .all
    @State private var isConnected: Bool = false
    @State private var errorMessage: String?
    @State private var showingError = false

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()

    var body: some View {
        VStack(spacing: 0) {
            filterSection
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(uiColor: .systemGroupedBackground))

            Divider()

            if filteredAlerts.isEmpty {
                emptyStateView
            } else {
                alertsList
            }
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle("实时告警")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                connectionStatusView
            }
        }
        .onAppear {
            connectWebSocket()
        }
        .onDisappear {
            disconnectWebSocket()
        }
        .alert("连接错误", isPresented: $showingError) {
            Button("确定", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "")
        }
    }

    // MARK: - Filter Section

    private var filterSection: some View {
        VStack(spacing: 12) {
            HStack {
                Text("类型")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.secondary)

                Picker("类型", selection: $selectedType) {
                    ForEach(LiveAlertTypeFilter.allCases) { filter in
                        Text(filter.title).tag(filter)
                    }
                }
                .pickerStyle(.segmented)
            }

            HStack {
                Text("严重度")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.secondary)

                Picker("严重度", selection: $selectedSeverity) {
                    ForEach(SeverityFilter.allCases) { filter in
                        Text(filter.title).tag(filter)
                    }
                }
                .pickerStyle(.segmented)
            }
        }
    }

    // MARK: - Connection Status

    private var connectionStatusView: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(isConnected ? Color.green : Color.gray)
                .frame(width: 8, height: 8)

            Text(isConnected ? "在线" : "离线")
                .font(.caption)
                .foregroundStyle(isConnected ? .green : .secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(Capsule())
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: isConnected ? "bell.slash.fill" : "wifi.slash")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)

            Text(isConnected ? "暂无告警" : "未连接")
                .font(.headline)
                .foregroundStyle(.secondary)

            Text(isConnected ? "等待实时告警数据..." : "正在连接告警服务...")
                .font(.subheadline)
                .foregroundStyle(.tertiary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Alerts List

    private var alertsList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(filteredAlerts) { alert in
                    NavigationLink {
                        LiveAlertDetailView(alert: alert)
                    } label: {
                        LiveAlertRow(alert: alert)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
    }

    private var filteredAlerts: [LiveAlert] {
        alerts.filter { alert in
            let typeMatch = selectedType.matches(alert)
            let severityMatch = selectedSeverity.matches(alert)
            return typeMatch && severityMatch
        }
    }

    // MARK: - WebSocket Connection

    private func connectWebSocket() {
        guard let token = UserDefaults.standard.string(forKey: "auth_token") else {
            errorMessage = "未找到认证令牌"
            showingError = true
            return
        }

        // Set up event handlers
        webSocketService.setHandlers(
            onMessage: { [weak self] message in
                self?.handleWebSocketMessage(message)
            },
            onOpen: { [weak self] in
                Task { @MainActor in
                    self?.isConnected = true
                }
            },
            onClose: { [weak self] error in
                Task { @MainActor in
                    self?.isConnected = false
                    if let error = error {
                        self?.errorMessage = "连接关闭: \(error.localizedDescription)"
                    }
                }
            },
            onFailure: { [weak self] error in
                Task { @MainActor in
                    self?.isConnected = false
                    self?.errorMessage = "WebSocket 错误: \(error.localizedDescription)"
                    self?.showingError = true
                }
            }
        )

        // Connect to WebSocket endpoint
        Task {
            do {
                let baseURL = "https://api.openclaw.ai"
                    .replacingOccurrences(of: "https://", with: "wss://")
                    .replacingOccurrences(of: "http://", with: "ws://")
                let wsURL = "\(baseURL)/ws/alerts"

                try await webSocketService.connect(to: wsURL, token: token)
            } catch {
                await MainActor.run {
                    errorMessage = "无法连接到告警服务: \(error.localizedDescription)"
                    showingError = true
                }
            }
        }
    }

    private func disconnectWebSocket() {
        webSocketService.disconnect()
        isConnected = false
    }

    private func handleWebSocketMessage(_ message: WebSocketService.Message) {
        let text: String
        switch message {
        case .text(let str):
            text = str
        case .data(let data):
            guard let str = String(data: data, encoding: .utf8) else { return }
            text = str
        }

        guard let data = text.data(using: .utf8) else { return }

        do {
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601

            let newAlert = try decoder.decode(LiveAlert.self, from: data)

            Task { @MainActor in
                // Insert at the beginning (most recent first)
                alerts.insert(newAlert, at: 0)

                // Keep only the last 100 alerts
                if alerts.count > 100 {
                    alerts.removeLast(alerts.count - 100)
                }
            }
        } catch {
            print("Failed to parse alert: \(error)")
        }
    }
}

// MARK: - Live Alert Row

private struct LiveAlertRow: View {
    let alert: LiveAlert

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()

    var body: some View {
        HStack(spacing: 12) {
            // Alert icon with severity color
            ZStack {
                Circle()
                    .fill(alert.severity.color.opacity(0.15))
                    .frame(width: 48, height: 48)

                Image(systemName: alert.typeIconName)
                    .font(.system(size: 20))
                    .foregroundStyle(alert.severity.color)
            }

            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Text(alert.displayType)
                        .font(.subheadline.weight(.semibold))

                    severityBadge
                }

                if !alert.h3Location.isEmpty {
                    HStack(spacing: 4) {
                        Image(systemName: "mappin.circle.fill")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(alert.h3Location)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Text(alert.description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)

                Text(Self.dateFormatter.string(from: alert.timestamp))
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }

            Spacer(minLength: 0)

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(16)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var severityBadge: some View {
        Text(alert.severity.displayName)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(alert.severity.color)
            .clipShape(Capsule())
    }
}

// MARK: - Alert Detail View

private struct LiveAlertDetailView: View {
    let alert: LiveAlert

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter
    }()

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(alert.severity.color.opacity(0.15))
                            .frame(width: 80, height: 80)

                        Image(systemName: alert.typeIconName)
                            .font(.system(size: 36))
                            .foregroundStyle(alert.severity.color)
                    }

                    Text(alert.displayType)
                        .font(.title2.weight(.semibold))

                    Text(alert.severity.displayName)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                        .background(alert.severity.color)
                        .clipShape(Capsule())
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
                .background(Color(uiColor: .secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                // Details
                VStack(spacing: 16) {
                    DetailRow(
                        icon: "clock.fill",
                        label: "时间",
                        value: Self.dateFormatter.string(from: alert.timestamp)
                    )

                    Divider()

                    DetailRow(
                        icon: "mappin.circle.fill",
                        label: "H3 位置",
                        value: alert.h3Location
                    )

                    Divider()

                    DetailRow(
                        icon: "info.circle.fill",
                        label: "描述",
                        value: alert.description
                    )
                }
                .padding(20)
                .background(Color(uiColor: .secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .padding()
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle("告警详情")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct DetailRow: View {
    let icon: String
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(.tint)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 4) {
                Text(label)
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Text(value)
                    .font(.body)
            }

            Spacer()
        }
    }
}

// MARK: - Models

struct LiveAlert: Identifiable, Codable {
    let id: String
    let type: LiveAlertType
    let h3Location: String
    let severity: AlertSeverity
    let description: String
    let timestamp: Date

    enum CodingKeys: String, CodingKey {
        case id
        case type
        case h3Location = "h3_location"
        case severity
        case description
        case timestamp
    }

    var displayType: String {
        type.displayName
    }

    var typeIconName: String {
        type.iconName
    }
}

enum LiveAlertType: String, Codable {
    case motionDetected = "motion_detected"
    case personDetected = "person_detected"
    case vehicleDetected = "vehicle_detected"
    case soundAlert = "sound_alert"
    case communityAlert = "community_alert"

    var displayName: String {
        switch self {
        case .motionDetected:
            return "运动检测"
        case .personDetected:
            return "人员检测"
        case .vehicleDetected:
            return "车辆检测"
        case .soundAlert:
            return "声音告警"
        case .communityAlert:
            return "社区告警"
        }
    }

    var iconName: String {
        switch self {
        case .motionDetected:
            return "waveform.path.ecg"
        case .personDetected:
            return "person.fill"
        case .vehicleDetected:
            return "car.fill"
        case .soundAlert:
            return "speaker.wave.3.fill"
        case .communityAlert:
            return "exclamationmark.triangle.fill"
        }
    }
}

enum AlertSeverity: String, Codable {
    case low
    case medium
    case high
    case critical

    var displayName: String {
        switch self {
        case .low:
            return "低"
        case .medium:
            return "中"
        case .high:
            return "高"
        case .critical:
            return "紧急"
        }
    }

    var color: Color {
        switch self {
        case .low:
            return .blue
        case .medium:
            return .yellow
        case .high:
            return .orange
        case .critical:
            return .red
        }
    }
}

// MARK: - Filters

enum LiveAlertTypeFilter: String, CaseIterable, Identifiable {
    case all
    case motion
    case person
    case vehicle
    case sound
    case community

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all:
            return "全部"
        case .motion:
            return "运动"
        case .person:
            return "人员"
        case .vehicle:
            return "车辆"
        case .sound:
            return "声音"
        case .community:
            return "社区"
        }
    }

    func matches(_ alert: LiveAlert) -> Bool {
        switch self {
        case .all:
            return true
        case .motion:
            return alert.type == .motionDetected
        case .person:
            return alert.type == .personDetected
        case .vehicle:
            return alert.type == .vehicleDetected
        case .sound:
            return alert.type == .soundAlert
        case .community:
            return alert.type == .communityAlert
        }
    }
}

enum SeverityFilter: String, CaseIterable, Identifiable {
    case all
    case low
    case medium
    case high
    case critical

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all:
            return "全部"
        case .low:
            return "低"
        case .medium:
            return "中"
        case .high:
            return "高"
        case .critical:
            return "紧急"
        }
    }

    func matches(_ alert: LiveAlert) -> Bool {
        switch self {
        case .all:
            return true
        default:
            return alert.severity.rawValue == rawValue
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        LiveAlertFeedView()
    }
}

#Preview("Alert Row") {
    VStack(spacing: 12) {
        LiveAlertRow(alert: LiveAlert(
            id: "1",
            type: .personDetected,
            h3Location: "8c2a100b1234567",
            severity: .high,
            description: "检测到未知人员在前门附近",
            timestamp: Date()
        ))

        LiveAlertRow(alert: LiveAlert(
            id: "2",
            type: .vehicleDetected,
            h3Location: "8c2a100b7654321",
            severity: .medium,
            description: "车辆停在车道上超过10分钟",
            timestamp: Date().addingTimeInterval(-300)
        ))

        LiveAlertRow(alert: LiveAlert(
            id: "3",
            type: .soundAlert,
            h3Location: "8c2a100b1111111",
            severity: .critical,
            description: "检测到玻璃破碎声",
            timestamp: Date().addingTimeInterval(-600)
        ))

        LiveAlertRow(alert: LiveAlert(
            id: "4",
            type: .motionDetected,
            h3Location: "8c2a100b2222222",
            severity: .low,
            description: "后院检测到运动",
            timestamp: Date().addingTimeInterval(-900)
        ))
    }
    .padding()
    .background(Color(uiColor: .systemGroupedBackground))
}

#Preview("Alert Detail") {
    NavigationStack {
        LiveAlertDetailView(alert: LiveAlert(
            id: "1",
            type: .personDetected,
            h3Location: "8c2a100b1234567",
            severity: .high,
            description: "检测到未知人员在前门附近徘徊，可能存在安全风险。建议立即查看监控录像。",
            timestamp: Date()
        ))
    }
}
