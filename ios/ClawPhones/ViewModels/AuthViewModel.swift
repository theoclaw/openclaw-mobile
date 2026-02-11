//
//  AuthViewModel.swift
//  ClawPhones
//

import SwiftUI

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var isAuthenticated: Bool
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    init() {
        self.isAuthenticated = DeviceConfig.shared.isProvisioned
    }

    func refreshAuthState() {
        isAuthenticated = DeviceConfig.shared.isProvisioned
    }

    func login(email: String, password: String) async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }

        do {
            let payload = try await OpenClawAPI.shared.login(email: email, password: password)
            DeviceConfig.shared.saveUserToken(payload.token)
            isAuthenticated = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func register(email: String, password: String, name: String?) async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }

        do {
            let payload = try await OpenClawAPI.shared.register(email: email, password: password, name: name)
            DeviceConfig.shared.saveUserToken(payload.token)
            isAuthenticated = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func loginWithApple(identityToken: String) async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }

        do {
            let payload = try await OpenClawAPI.shared.loginWithApple(identityToken: identityToken)
            DeviceConfig.shared.saveUserToken(payload.token)
            isAuthenticated = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func logout() {
        // Clear Keychain + reset state
        DeviceConfig.shared.clearTokens()
        isLoading = false
        errorMessage = nil
        isAuthenticated = false
    }
}
