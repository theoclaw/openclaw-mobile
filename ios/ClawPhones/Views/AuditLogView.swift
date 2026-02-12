//
//  AuditLogView.swift
//  ClawPhones
//

import SwiftUI

enum AuditActionType: String, CaseIterable, Identifiable {
    case dataAccess = "data_access"
    case settingChange = "setting_change"
    case dataExport = "data_export"
    case dataDeletion = "data_deletion"
    case consentChange = "consent_change"
    case accountAction = "account_action"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .dataAccess: return "数据访问"
        case .settingChange: return "设置变更"
        case .dataExport: return "数据导出"
        case .dataDeletion: return "数据删除"
        case .consentChange: return "权限变更"
        case .accountAction: return "账户操作"
        }
    }

    var systemImage: String {
        switch self {
        case .dataAccess: return "doc.text.fill"
        case .settingChange: return "gearshape.fill"
        case .dataExport: return "square.and.arrow.up.fill"
        case .dataDeletion: return "trash.fill"
        case .consentChange: return "hand.raised.fill"
        case .accountAction: return "person.crop.circle.fill"
        }
    }

    var color: Color {
        switch self {
        case .dataAccess: return .blue
        case .settingChange: return .orange
        case .dataExport: return .green
        case .dataDeletion: return .red
        case .consentChange: return .purple
        case .accountAction: return .gray
        }
    }
}

struct AuditLogEntry: Identifiable {
    let id: String
    let type: AuditActionType
    let description: String
    let timestamp: Date
    let details: String?
    var isExpanded: Bool = false

    var formattedTimestamp: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter.string(from: timestamp)
    }

    var relativeTime: String {
        let now = Date()
        let interval = now.timeIntervalSince(timestamp)

        if interval < 60 { return "刚刚" }
        if interval < 3600 { return "\(Int(interval / 60)) 分钟前" }
        if interval < 86400 { return "\(Int(interval / 3600)) 小时前" }
        if interval < 604800 { return "\(Int(interval / 86400)) 天前" }

        let daysAgo = Int(interval / 86400)
        if daysAgo < 30 { return "\(daysAgo) 天前" }
        if daysAgo < 365 { return "\(daysAgo / 30) 个月前" }
        return "\(daysAgo / 365) 年前"
    }
}

struct AuditLogView: View {
    @State private var entries: [AuditLogEntry] = []
    @State private var selectedFilter: AuditActionType?
    @State private var startDate: Date = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()
    @State private var endDate: Date = Date()
    @State private var showFilterSheet = false
    @State private var showDatePicker = false
    @State private var isLoading = false

    private var filteredEntries: [AuditLogEntry] {
        entries.filter { entry in
            let matchesType = selectedFilter == nil || entry.type == selectedFilter
            let matchesDate = entry.timestamp >= startDate && entry.timestamp <= endDate
            return matchesType && matchesDate
        }
        .sorted { $0.timestamp > $1.timestamp }
    }

    var body: some View {
        List {
            if filteredEntries.isEmpty {
                ContentUnavailableView(
                    "暂无日志",
                    systemImage: "doc.text.below.ecg",
                    description: Text("在选定的时间范围内没有找到相关操作记录。")
                )
            } else {
                ForEach(filteredEntries) { entry in
                    AuditLogRow(entry: entry) {
                        if let index = entries.firstIndex(where: { $0.id == entry.id }) {
                            entries[index].isExpanded.toggle()
                        }
                    }
                }
            }
        }
        .navigationTitle("审计日志")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showFilterSheet = true
                } label: {
                    Image(systemName: selectedFilter == nil ? "line.3.horizontal.decrease.circle" : "line.3.horizontal.decrease.circle.fill")
                }
            }
        }
        .sheet(isPresented: $showFilterSheet) {
            NavigationStack {
                filterSheet
            }
        }
        .task {
            await loadAuditLogs()
        }
        .refreshable {
            await loadAuditLogs()
        }
        .overlay {
            if isLoading {
                ProgressView()
                    .padding()
                    .background(.regularMaterial)
                    .cornerRadius(10)
            }
        }
    }

    @ViewBuilder
    private var filterSheet: some View {
        Form {
            Section("操作类型") {
                Button {
                    selectedFilter = nil
                    showFilterSheet = false
                } label: {
                    HStack {
                        Image(systemName: "circle")
                        Text("全部")
                    }
                }
                .foregroundStyle(.primary)

                ForEach(AuditActionType.allCases) { type in
                    Button {
                        selectedFilter = type
                        showFilterSheet = false
                    } label: {
                        HStack {
                            Image(systemName: selectedFilter == type ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(selectedFilter == type ? .blue : .gray)
                            Text(type.displayName)
                        }
                    }
                    .foregroundStyle(.primary)
                }
            }

            Section("时间范围") {
                DatePicker("开始日期", selection: $startDate, in: ...Date(), displayedComponents: .date)
                DatePicker("结束日期", selection: $endDate, in: startDate...Date(), displayedComponents: .date)
            }

            Section {
                Button("重置筛选", role: .destructive) {
                    resetFilters()
                }
            }
        }
        .navigationTitle("筛选日志")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("取消") {
                    showFilterSheet = false
                }
            }
        }
    }

    private func loadAuditLogs() async {
        isLoading = true

        // Simulate API call
        try? await Task.sleep(nanoseconds: 500_000_000)

        await MainActor.run {
            entries = generateMockLogs()
            isLoading = false
        }
    }

    private func generateMockLogs() -> [AuditLogEntry] {
        let now = Date()
        var logs: [AuditLogEntry] = []
        let actions: [(AuditActionType, String, String?)] = [
            (.dataAccess, "查看隐私设置", nil),
            (.settingChange, "更新位置精度为城市级", "从: 精确位置\n到: 城市级"),
            (.consentChange, "撤销分析数据共享权限", "功能: 使用分析"),
            (.dataExport, "请求数据导出", "导出ID: export_12345"),
            (.dataDeletion, "删除30天前的对话记录", "删除数量: 15 条"),
            (.settingChange, "启用自动删除过期数据", "保留天数: 30 天"),
            (.accountAction, "修改账户密码", nil),
            (.consentChange, "授予产品更新通知权限", "功能: 产品更新通知"),
            (.dataAccess, "访问用户个人资料", "IP: 192.168.1.100"),
            (.settingChange, "启用人脸模糊功能", nil),
            (.dataDeletion, "清除所有缓存数据", "释放空间: 128.5 MB"),
            (.consentChange, "撤销崩溃报告权限", "功能: 崩溃报告"),
        ]

        for (index, (type, description, details)) in actions.enumerated() {
            let timestamp = Calendar.current.date(byAdding: .hour, value: -index * 2, to: now) ?? now
            logs.append(AuditLogEntry(
                id: UUID().uuidString,
                type: type,
                description: description,
                timestamp: timestamp,
                details: details
            ))
        }

        return logs
    }

    private func resetFilters() {
        selectedFilter = nil
        startDate = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()
        endDate = Date()
    }
}

struct AuditLogRow: View {
    let entry: AuditLogEntry
    let onTap: () -> Void

    var body: some View {
        Button {
            onTap()
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(entry.type.color.opacity(0.15))
                            .frame(width: 40, height: 40)

                        Image(systemName: entry.type.systemImage)
                            .font(.system(size: 18))
                            .foregroundStyle(entry.type.color)
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(entry.type.displayName)
                                .font(.caption)
                                .foregroundStyle(.secondary)

                            Spacer()

                            Text(entry.relativeTime)
                                .font(.caption2)
                                .foregroundStyle(.tertiary)
                        }

                        Text(entry.description)
                            .font(.subheadline)
                            .foregroundStyle(.primary)
                    }

                    Spacer()

                    Image(systemName: entry.isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)

                if entry.isExpanded, let details = entry.details {
                    Divider()

                    VStack(alignment: .leading, spacing: 4) {
                        Text("详细信息")
                            .font(.caption)
                            .foregroundStyle(.secondary)

                        Text(details)
                            .font(.caption)
                            .foregroundStyle(.primary)
                    }

                    HStack {
                        Text("时间戳")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Spacer()
                        Text(entry.formattedTimestamp)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        AuditLogView()
    }
}
