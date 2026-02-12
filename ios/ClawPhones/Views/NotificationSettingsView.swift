//
//  NotificationSettingsView.swift
//  ClawPhones
//
//  Notification preferences and quiet hours management
//

import SwiftUI

enum NotificationSound: String, CaseIterable, Identifiable {
    case defaultSound = "default"
    case chime = "chime"
    case bell = "bell"
    case ping = "ping"
    case silent = "silent"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .defaultSound: return "默认"
        case .chime: return "提示音"
        case .bell: return "铃声"
        case .ping: return "叮咚"
        case .silent: return "静音"
        }
    }

    var systemImage: String {
        switch self {
        case .defaultSound: return "speaker.wave.2.fill"
        case .chime: return "bell.fill"
        case .bell: return "bell.badge.fill"
        case .ping: return "bell.circle.fill"
        case .silent: return "speaker.slash.fill"
        }
    }
}

struct NotificationSettings {
    var communityAlertsEnabled: Bool = true
    var taskNotificationsEnabled: Bool = true
    var edgeComputeEnabled: Bool = true
    var securityAlertsEnabled: Bool = true

    var quietHoursEnabled: Bool = false
    var quietHoursStart: Date = Calendar.current.date(from: DateComponents(hour: 22, minute: 0)) ?? Date()
    var quietHoursEnd: Date = Calendar.current.date(from: DateComponents(hour: 8, minute: 0)) ?? Date()

    var soundEnabled: Bool = true
    var selectedSound: NotificationSound = .defaultSound
    var vibrationEnabled: Bool = true

    static let `default` = NotificationSettings()
}

struct NotificationSettingsView: View {
    @StateObject private var pushService = PushNotificationService.shared
    @State private var settings = NotificationSettings.default
    @State private var showPermissionAlert = false
    @State private var isSaving = false

    var body: some View {
        Form {
            // MARK: - Permission Status
            Section {
                HStack(spacing: 12) {
                    Image(systemName: pushService.isPermissionGranted ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                        .font(.title2)
                        .foregroundStyle(pushService.isPermissionGranted ? .green : .orange)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(pushService.isPermissionGranted ? "推送通知已启用" : "推送通知未启用")
                            .font(.headline)
                        Text(pushService.isPermissionGranted ? "您将收到重要通知" : "前往系统设置以启用通知")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    if !pushService.isPermissionGranted {
                        Button {
                            pushService.openAppSettings()
                        } label: {
                            Text("设置")
                                .font(.subheadline)
                                .fontWeight(.medium)
                        }
                        .buttonStyle(.bordered)
                    }
                }
            } footer: {
                Text("推送通知权限在系统设置中管理。")
                    .font(.footnote)
            }

            // MARK: - Notification Categories
            Section("通知类别") {
                NotificationToggleRow(
                    title: "社区提醒",
                    description: "社区消息和活动通知",
                    icon: "person.3.fill",
                    isOn: $settings.communityAlertsEnabled
                )

                NotificationToggleRow(
                    title: "任务通知",
                    description: "任务分配、完成和过期提醒",
                    icon: "list.bullet.clipboard.fill",
                    isOn: $settings.taskNotificationsEnabled
                )

                NotificationToggleRow(
                    title: "边缘计算",
                    description: "计算任务状态更新",
                    icon: "cpu.fill",
                    isOn: $settings.edgeComputeEnabled
                )

                NotificationToggleRow(
                    title: "安全提醒",
                    description: "设备安全和异常活动警报",
                    icon: "shield.fill",
                    isOn: $settings.securityAlertsEnabled,
                    iconColor: .red
                )
            } header: {
                Text("通知类别")
            } footer: {
                Text("选择您希望接收的通知类型。")
                    .font(.footnote)
            }

            // MARK: - Quiet Hours
            Section("免打扰时段") {
                Toggle("启用免打扰", isOn: $settings.quietHoursEnabled)

                if settings.quietHoursEnabled {
                    DatePicker(
                        "开始时间",
                        selection: $settings.quietHoursStart,
                        displayedComponents: .hourAndMinute
                    )

                    DatePicker(
                        "结束时间",
                        selection: $settings.quietHoursEnd,
                        displayedComponents: .hourAndMinute
                    )

                    HStack {
                        Image(systemName: "moon.fill")
                            .foregroundStyle(.indigo)
                        Text(quietHoursDescription)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            } header: {
                Text("免打扰时段")
            } footer: {
                Text("在免打扰时段内，除安全提醒外的通知将被静音。")
                    .font(.footnote)
            }

            // MARK: - Sound & Vibration
            Section("声音与振动") {
                Toggle("启用声音", isOn: $settings.soundEnabled)

                if settings.soundEnabled {
                    Picker("通知声音", selection: $settings.selectedSound) {
                        ForEach(NotificationSound.allCases) { sound in
                            Label(sound.displayName, systemImage: sound.systemImage)
                                .tag(sound)
                        }
                    }
                }

                Toggle("启用振动", isOn: $settings.vibrationEnabled)
            } header: {
                Text("声音与振动")
            } footer: {
                Text("自定义通知的声音和振动反馈。")
                    .font(.footnote)
            }

            // MARK: - Save Button
            Section {
                Button {
                    saveSettings()
                } label: {
                    HStack {
                        Spacer()
                        if isSaving {
                            ProgressView()
                                .controlSize(.small)
                                .padding(.trailing, 8)
                        }
                        Text(isSaving ? "保存中..." : "保存设置")
                            .fontWeight(.medium)
                        Spacer()
                    }
                }
                .disabled(isSaving || !pushService.isPermissionGranted)
            }
        }
        .navigationTitle("通知设置")
        .task {
            await pushService.getNotificationSettings()
            loadSettings()
        }
        .alert("需要通知权限", isPresented: $showPermissionAlert) {
            Button("前往设置") {
                pushService.openAppSettings()
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("请在系统设置中启用通知权限以接收推送通知。")
        }
    }

    private var quietHoursDescription: String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        let start = formatter.string(from: settings.quietHoursStart)
        let end = formatter.string(from: settings.quietHoursEnd)
        return "免打扰时段: \(start) - \(end)"
    }

    private func loadSettings() {
        // Load from UserDefaults
        settings.communityAlertsEnabled = UserDefaults.standard.object(forKey: "notif_community") as? Bool ?? true
        settings.taskNotificationsEnabled = UserDefaults.standard.object(forKey: "notif_tasks") as? Bool ?? true
        settings.edgeComputeEnabled = UserDefaults.standard.object(forKey: "notif_edge") as? Bool ?? true
        settings.securityAlertsEnabled = UserDefaults.standard.object(forKey: "notif_security") as? Bool ?? true
        settings.quietHoursEnabled = UserDefaults.standard.bool(forKey: "notif_quiet_enabled")
        settings.soundEnabled = UserDefaults.standard.object(forKey: "notif_sound_enabled") as? Bool ?? true
        settings.vibrationEnabled = UserDefaults.standard.object(forKey: "notif_vibration") as? Bool ?? true

        if let soundRaw = UserDefaults.standard.string(forKey: "notif_sound_type"),
           let sound = NotificationSound(rawValue: soundRaw) {
            settings.selectedSound = sound
        }

        if let quietStart = UserDefaults.standard.object(forKey: "notif_quiet_start") as? Date {
            settings.quietHoursStart = quietStart
        }
        if let quietEnd = UserDefaults.standard.object(forKey: "notif_quiet_end") as? Date {
            settings.quietHoursEnd = quietEnd
        }
    }

    private func saveSettings() {
        guard pushService.isPermissionGranted else {
            showPermissionAlert = true
            return
        }

        isSaving = true

        Task {
            // Simulate API call to save settings
            try? await Task.sleep(nanoseconds: 1_000_000_000)

            // Save settings to UserDefaults
            UserDefaults.standard.set(settings.communityAlertsEnabled, forKey: "notif_community")
            UserDefaults.standard.set(settings.taskNotificationsEnabled, forKey: "notif_tasks")
            UserDefaults.standard.set(settings.edgeComputeEnabled, forKey: "notif_edge")
            UserDefaults.standard.set(settings.securityAlertsEnabled, forKey: "notif_security")
            UserDefaults.standard.set(settings.quietHoursEnabled, forKey: "notif_quiet_enabled")
            UserDefaults.standard.set(settings.quietHoursStart, forKey: "notif_quiet_start")
            UserDefaults.standard.set(settings.quietHoursEnd, forKey: "notif_quiet_end")
            UserDefaults.standard.set(settings.soundEnabled, forKey: "notif_sound_enabled")
            UserDefaults.standard.set(settings.selectedSound.rawValue, forKey: "notif_sound_type")
            UserDefaults.standard.set(settings.vibrationEnabled, forKey: "notif_vibration")

            print("Notification settings saved")
            print("- Community Alerts: \(settings.communityAlertsEnabled)")
            print("- Task Notifications: \(settings.taskNotificationsEnabled)")
            print("- Edge Compute: \(settings.edgeComputeEnabled)")
            print("- Security Alerts: \(settings.securityAlertsEnabled)")
            print("- Quiet Hours: \(settings.quietHoursEnabled)")
            print("- Sound: \(settings.soundEnabled) (\(settings.selectedSound.displayName))")
            print("- Vibration: \(settings.vibrationEnabled)")

            await MainActor.run {
                isSaving = false
            }
        }
    }
}

// MARK: - NotificationToggleRow

private struct NotificationToggleRow: View {
    let title: String
    let description: String
    let icon: String
    @Binding var isOn: Bool
    var iconColor: Color = .blue

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(iconColor)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Toggle("", isOn: $isOn)
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        NotificationSettingsView()
    }
}
