//
//  UsageDashboardView.swift
//  ClawPhones
//

import SwiftUI
import Charts

struct UsageDashboardView: View {
    @State private var selectedRange: DateRange = .week
    @State private var customStartDate = Date()
    @State private var customEndDate = Date()
    @State private var showCustomRangeSheet = false
    @State private var usageData: [DailyUsage] = []
    @State private var topEndpoints: [EndpointUsage] = []
    @State private var latencyStats: LatencyStats?
    @State private var rateLimit: RateLimitInfo?
    @State private var isLoading = true

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                dateRangePicker

                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, minHeight: 200)
                } else {
                    apiCallsChart

                    if !topEndpoints.isEmpty {
                        topEndpointsSection
                    }

                    if let latency = latencyStats {
                        latencySection(latency)
                    }

                    if let limit = rateLimit {
                        rateLimitSection(limit)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Usage Dashboard")
        .task {
            await loadData()
        }
        .refreshable {
            await loadData()
        }
        .sheet(isPresented: $showCustomRangeSheet) {
            CustomDateRangeSheet(
                startDate: $customStartDate,
                endDate: $customEndDate,
                isPresented: $showCustomRangeSheet
            ) {
                selectedRange = .custom
                Task { await loadData() }
            }
        }
    }

    private var dateRangePicker: some View {
        Picker("Date Range", selection: $selectedRange) {
            Text("Today").tag(DateRange.today)
            Text("This Week").tag(DateRange.week)
            Text("This Month").tag(DateRange.month)
            Text("Custom").tag(DateRange.custom)
        }
        .pickerStyle(.segmented)
        .onChange(of: selectedRange) { _, newValue in
            if newValue == .custom {
                showCustomRangeSheet = true
            } else {
                Task { await loadData() }
            }
        }
    }

    private var apiCallsChart: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("API Calls")
                .font(.headline)

            VStack(alignment: .leading, spacing: 4) {
                Text("\(totalCalls, specifier: "%,d") calls")
                    .font(.title2)
                    .bold()

                Text(dateRangeText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Chart(usageData) { item in
                BarMark(
                    x: .value("Date", item.date),
                    y: .value("Calls", item.calls)
                )
                .foregroundStyle(.blue.gradient)
            }
            .frame(height: 200)
            .chartXAxis {
                AxisMarks(values: .stride(by: .day)) { _ in
                    AxisValueLabel(format: .dateTime.month().day())
                }
            }
            .chartYAxis {
                AxisMarks(position: .leading)
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var topEndpointsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Top Endpoints")
                .font(.headline)

            ForEach(topEndpoints.prefix(5)) { endpoint in
                HStack {
                    Text(endpoint.path)
                        .font(.system(.subheadline, design: .monospaced))
                        .lineLimit(1)

                    Spacer()

                    VStack(alignment: .trailing, spacing: 2) {
                        Text("\(endpoint.calls, specifier: "%,d")")
                            .font(.subheadline)
                            .bold()

                        Text("\(usagePercentage(endpoint.calls))%")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    let progress = usagePercentage(endpoint.calls) / 100.0
                    GeometryReader { geometry in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color(uiColor: .tertiarySystemFill))

                            RoundedRectangle(cornerRadius: 4)
                                .fill(.blue)
                                .frame(width: geometry.size.width * progress)
                        }
                    }
                    .frame(width: 60, height: 6)
                }
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func latencySection(_ latency: LatencyStats) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Latency Stats")
                .font(.headline)

            HStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Average")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Text("\(Int(latency.average))ms")
                        .font(.title3)
                        .bold()
                }

                Spacer()

                VStack(alignment: .leading, spacing: 4) {
                    Text("P50")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Text("\(Int(latency.p50))ms")
                        .font(.title3)
                        .bold()
                }

                Spacer()

                VStack(alignment: .leading, spacing: 4) {
                    Text("P95")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Text("\(Int(latency.p95))ms")
                        .font(.title3)
                        .bold()
                }

                Spacer()

                VStack(alignment: .leading, spacing: 4) {
                    Text("P99")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Text("\(Int(latency.p99))ms")
                        .font(.title3)
                        .bold()
                }
            }

            HStack(spacing: 8) {
                Circle()
                    .fill(latencyStatusColor)
                    .frame(width: 10, height: 10)

                Text(latencyStatusText)
                    .font(.subheadline)
                    .foregroundStyle(latencyStatusColor)
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func rateLimitSection(_ limit: RateLimitInfo) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Rate Limit")
                .font(.headline)

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("API Calls")
                    Spacer()
                    Text("\(limit.current) / \(limit.limit)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                ProgressView(value: Double(limit.current), total: Double(limit.limit))
                    .tint(rateLimitColor)

                HStack {
                    Text("\(limit.remaining) remaining")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Spacer()

                    Text("Resets: \(limit.resetDate, format: .dateTime.hour().minute())")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var totalCalls: Int {
        usageData.reduce(0) { $0 + $1.calls }
    }

    private var dateRangeText: String {
        switch selectedRange {
        case .today:
            return "Today"
        case .week:
            return "This Week"
        case .month:
            return "This Month"
        case .custom:
            let formatter = DateFormatter()
            formatter.dateStyle = .short
            return "\(formatter.string(from: customStartDate)) - \(formatter.string(from: customEndDate))"
        }
    }

    private func usagePercentage(_ calls: Int) -> Double {
        guard totalCalls > 0 else { return 0 }
        return Double(calls) / Double(totalCalls) * 100
    }

    private var latencyStatusColor: Color {
        guard let latency = latencyStats else { return .secondary }
        if latency.p99 < 500 { return .green }
        else if latency.p99 < 1000 { return .orange }
        else { return .red }
    }

    private var latencyStatusText: String {
        guard let latency = latencyStats else { return "Unknown" }
        if latency.p99 < 500 { return "Excellent" }
        else if latency.p99 < 1000 { return "Good" }
        else { return "Needs Attention" }
    }

    private var rateLimitColor: Color {
        guard let limit = rateLimit else { return .blue }
        let percentage = Double(limit.current) / Double(limit.limit)
        if percentage < 0.5 { return .green }
        else if percentage < 0.8 { return .orange }
        else { return .red }
    }

    @MainActor
    private func loadData() async {
        isLoading = true
        defer { isLoading = false }

        // TODO: Implement API calls based on selected range
        let (startDate, endDate) = dateRangeForSelection()
        usageData = await fetchUsageData(from: startDate, to: endDate)
        topEndpoints = await fetchTopEndpoints(from: startDate, to: endDate)
        latencyStats = await fetchLatencyStats(from: startDate, to: endDate)
        rateLimit = await fetchRateLimitInfo()
    }

    private func dateRangeForSelection() -> (Date, Date) {
        let calendar = Calendar.current
        let now = Date()

        switch selectedRange {
        case .today:
            let start = calendar.startOfDay(for: now)
            return (start, now)

        case .week:
            let start = calendar.date(byAdding: .day, value: -7, to: now) ?? now
            return (start, now)

        case .month:
            let start = calendar.date(byAdding: .day, value: -30, to: now) ?? now
            return (start, now)

        case .custom:
            return (customStartDate, customEndDate)
        }
    }

    private func fetchUsageData(from start: Date, to end: Date) async -> [DailyUsage] {
        // TODO: Implement API call
        return []
    }

    private func fetchTopEndpoints(from start: Date, to end: Date) async -> [EndpointUsage] {
        // TODO: Implement API call
        return []
    }

    private func fetchLatencyStats(from start: Date, to end: Date) async -> LatencyStats? {
        // TODO: Implement API call
        return nil
    }

    private func fetchRateLimitInfo() async -> RateLimitInfo? {
        // TODO: Implement API call
        return nil
    }
}

// MARK: - Custom Date Range Sheet

private struct CustomDateRangeSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var startDate: Date
    @Binding var endDate: Date
    @Binding var isPresented: Bool
    let onApply: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker("Start Date", selection: $startDate, displayedComponents: .date)
                    DatePicker("End Date", selection: $endDate, displayedComponents: .date)
                } header: {
                    Text("Select Range")
                } footer: {
                    if startDate > endDate {
                        Text("Start date must be before end date")
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("Custom Range")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Apply") {
                        dismiss()
                        onApply()
                    }
                    .disabled(startDate > endDate)
                }
            }
        }
    }
}

// MARK: - Models

struct DailyUsage: Identifiable, Codable {
    let id = UUID()
    let date: Date
    let calls: Int
    let errors: Int
}

struct LatencyStats: Codable {
    let average: Double
    let p50: Double
    let p95: Double
    let p99: Double
    let min: Double
    let max: Double
}

struct RateLimitInfo: Codable {
    let limit: Int
    let current: Int
    let remaining: Int
    let resetDate: Date
}

enum DateRange: String, CaseIterable {
    case today = "today"
    case week = "week"
    case month = "month"
    case custom = "custom"
}
