//
//  PluginMarketView.swift
//  ClawPhones
//

import SwiftUI

struct PluginMarketView: View {
    @State private var plugins: [Plugin] = []
    @State private var filteredPlugins: [Plugin] = []
    @State private var installedPlugins: Set<String> = []
    @State private var isLoading = true
    @State private var searchText = ""
    @State private var showInstalledOnly = false
    @State private var errorMessage: String?
    @State private var showingError = false

    private let columns = [
        GridItem(.adaptive(minimum: 160, maximum: 200), spacing: 16)
    ]

    var body: some View {
        VStack(spacing: 0) {
            searchBar

            filterToggle

            if isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if filteredPlugins.isEmpty {
                emptyState
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 16) {
                        ForEach(filteredPlugins) { plugin in
                            PluginCard(
                                plugin: plugin,
                                isInstalled: installedPlugins.contains(plugin.id)
                            ) {
                                Task {
                                    await togglePlugin(plugin)
                                }
                            }
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("Plugin Market")
        .task {
            await loadData()
        }
        .refreshable {
            await loadData()
        }
        .alert("Error", isPresented: $showingError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)

            TextField("Search plugins...", text: $searchText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()

            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .padding(.horizontal)
        .padding(.top)
    }

    private var filterToggle: some View {
        HStack {
            Button {
                withAnimation {
                    showInstalledOnly.toggle()
                    filterPlugins()
                }
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: showInstalledOnly ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(showInstalledOnly ? .blue : .secondary)

                    Text("Installed Only")
                        .font(.subheadline)
                }
            }

            Spacer()

            if !installedPlugins.isEmpty {
                Text("\(installedPlugins.count) installed")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(uiColor: .tertiarySystemFill))
                    .clipShape(Capsule())
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "puzzlepiece.extension.fill")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)

            Text("No Plugins Found")
                .font(.headline)
                .foregroundStyle(.secondary)

            Text(showInstalledOnly
                 ? "You haven't installed any plugins yet"
                 : "Try adjusting your search criteria")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            if showInstalledOnly {
                Button("Show All Plugins") {
                    withAnimation {
                        showInstalledOnly = false
                        filterPlugins()
                    }
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    @MainActor
    private func loadData() async {
        isLoading = true
        defer { isLoading = false }

        plugins = await fetchPlugins()
        installedPlugins = await fetchInstalledPlugins()
        filterPlugins()
    }

    private func filterPlugins() {
        if searchText.isEmpty && !showInstalledOnly {
            filteredPlugins = plugins
        } else {
            filteredPlugins = plugins.filter { plugin in
                let matchesSearch = searchText.isEmpty ||
                    plugin.name.localizedCaseInsensitiveContains(searchText) ||
                    plugin.author.localizedCaseInsensitiveContains(searchText) ||
                    plugin.description.localizedCaseInsensitiveContains(searchText)

                let matchesFilter = !showInstalledOnly || installedPlugins.contains(plugin.id)

                return matchesSearch && matchesFilter
            }
        }
    }

    @MainActor
    private func togglePlugin(_ plugin: Plugin) async {
        if installedPlugins.contains(plugin.id) {
            await uninstallPlugin(plugin)
        } else {
            await installPlugin(plugin)
        }
    }

    @MainActor
    private func installPlugin(_ plugin: Plugin) async {
        // TODO: Implement installation
        installedPlugins.insert(plugin.id)
    }

    @MainActor
    private func uninstallPlugin(_ plugin: Plugin) async {
        // TODO: Implement uninstallation
        installedPlugins.remove(plugin.id)
    }

    private func fetchPlugins() async -> [Plugin] {
        // TODO: Implement API call
        return []
    }

    private func fetchInstalledPlugins() async -> Set<String> {
        // TODO: Implement API call
        return []
    }
}

// MARK: - Plugin Card

struct PluginCard: View {
    let plugin: Plugin
    let isInstalled: Bool
    let onAction: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                pluginIcon

                VStack(alignment: .leading, spacing: 4) {
                    Text(plugin.name)
                        .font(.headline)
                        .lineLimit(1)

                    Text(plugin.version)
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Text("by \(plugin.author)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()
            }

            Text(plugin.description)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(3)

            HStack(spacing: 8) {
                ForEach(plugin.categories, id: \.self) { category in
                    Text(category)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(Color.blue.opacity(0.2))
                        .foregroundStyle(.blue)
                        .clipShape(Capsule())
                }
            }

            Spacer()

            actionButton
        }
        .padding()
        .frame(height: 180)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }

    private var pluginIcon: some View {
        ZStack {
            Color.accentColor.opacity(0.2)

            if let icon = plugin.iconName {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(.accent)
            } else {
                Text(String(plugin.name.prefix(1)))
                    .font(.title2)
                    .bold()
                    .foregroundStyle(.accent)
            }
        }
        .frame(width: 48, height: 48)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var actionButton: some View {
        Button {
            onAction()
        } label: {
            HStack {
                Spacer()

                if isInstalled {
                    Label("Uninstall", systemImage: "trash")
                        .foregroundStyle(.red)
                } else {
                    Label("Install", systemImage: "download")
                }

                Spacer()
            }
        }
        .buttonStyle(.bordered)
        .controlSize(.small)
        .tint(isInstalled ? .red : .blue)
    }
}

// MARK: - Models

struct Plugin: Identifiable, Codable {
    let id: String
    let name: String
    let description: String
    let version: String
    let author: String
    let iconName: String?
    let categories: [String]
    let rating: Double
    let downloads: Int
    let lastUpdated: Date
    let minAppVersion: String
    let screenshotURLs: [String]?

    var displayRating: String {
        String(format: "%.1f", rating)
    }

    var displayDownloads: String {
        if downloads >= 1_000_000 {
            return "\(downloads / 1_000_000)M"
        } else if downloads >= 1_000 {
            return "\(downloads / 1_000)K"
        }
        return "\(downloads)"
    }
}

// MARK: - Plugin Service

enum PluginError: LocalizedError {
    case installationFailed
    case uninstallationFailed
    case networkError(Error)
    case notInstalled
    case alreadyInstalled
    case incompatibleVersion
    case unknown(String)

    var errorDescription: String? {
        switch self {
        case .installationFailed:
            return "Failed to install plugin"
        case .uninstallationFailed:
            return "Failed to uninstall plugin"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .notInstalled:
            return "Plugin is not installed"
        case .alreadyInstalled:
            return "Plugin is already installed"
        case .incompatibleVersion:
            return "Plugin is not compatible with this app version"
        case .unknown(let message):
            return message
        }
    }
}

actor PluginService {
    static let shared = PluginService()

    private init() {}

    func fetchAvailablePlugins() async -> [Plugin] {
        // TODO: Implement API call
        return []
    }

    func fetchInstalledPlugins() async -> [Plugin] {
        // TODO: Implement API call
        return []
    }

    func installPlugin(id: String) async -> Result<Void, PluginError> {
        // TODO: Implement API call
        return .failure(.unknown("Not implemented"))
    }

    func uninstallPlugin(id: String) async -> Result<Void, PluginError> {
        // TODO: Implement API call
        return .failure(.unknown("Not implemented"))
    }

    func updatePlugin(id: String) async -> Result<Void, PluginError> {
        // TODO: Implement API call
        return .failure(.unknown("Not implemented"))
    }

    func fetchPluginDetails(id: String) async -> PluginDetail? {
        // TODO: Implement API call
        return nil
    }
}

struct PluginDetail {
    let plugin: Plugin
    let readme: String
    let changelog: [ChangelogEntry]
    let permissions: [String]
    let fileSize: Int
}

struct ChangelogEntry: Identifiable {
    let id = UUID()
    let version: String
    let date: Date
    let changes: [String]
}
