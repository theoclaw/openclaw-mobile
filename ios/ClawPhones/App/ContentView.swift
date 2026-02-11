//
//  ContentView.swift
//  ClawPhones
//

import SwiftUI

struct ContentView: View {
    @StateObject private var auth = AuthViewModel()
    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding: Bool = false

    private enum Tab: Hashable {
        case chat
        case settings
    }

    @State private var selectedTab: Tab = .chat
    @State private var pendingSharedPayloadID: String?
    @State private var hasLoadedInitialSharedPayload = false

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                ConversationListView(pendingSharedPayloadID: $pendingSharedPayloadID)
            }
            .tabItem {
                Label("聊天", systemImage: "message")
            }
            .tag(Tab.chat)

            NavigationStack {
                SettingsView()
            }
            .tabItem {
                Label("设置", systemImage: "gearshape")
            }
            .tag(Tab.settings)
        }
        .environmentObject(auth)
        .fullScreenCover(isPresented: Binding(
            get: { hasSeenOnboarding && !auth.isAuthenticated },
            set: { _ in }
        )) {
            NavigationStack {
                LoginView()
            }
            .environmentObject(auth)
        }
        .fullScreenCover(isPresented: Binding(
            get: { !hasSeenOnboarding },
            set: { _ in }
        )) {
            OnboardingView {
                hasSeenOnboarding = true
            }
            .interactiveDismissDisabled()
        }
        .onReceive(NotificationCenter.default.publisher(for: Notification.Name("ClawPhonesAuthExpired"))) { _ in
            auth.refreshAuthState()
        }
        .onOpenURL { url in
            guard let payloadID = SharePayloadBridge.payloadID(from: url) else { return }
            selectedTab = .chat
            pendingSharedPayloadID = payloadID
        }
        .onAppear {
            auth.refreshAuthState()

            guard !hasLoadedInitialSharedPayload else { return }
            hasLoadedInitialSharedPayload = true

            if let payloadID = SharePayloadBridge.latestPayloadID() {
                pendingSharedPayloadID = payloadID
            }
        }
    }
}
