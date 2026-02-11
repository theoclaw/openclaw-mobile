//
//  AuthViewModel.swift
//  ClawPhones
//

import SwiftUI
import AuthenticationServices

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var isAuthenticated: Bool
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    private var authObserver: Any?

    init() {
        self.isAuthenticated = DeviceConfig.shared.isProvisioned
        authObserver = NotificationCenter.default.addObserver(
            forName: .clawPhonesAuthDidChange,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.refreshAuthState()
        }
    }

    deinit {
        if let obs = authObserver {
            NotificationCenter.default.removeObserver(obs)
        }
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
            DeviceConfig.shared.saveUserToken(payload.token, expiresAt: payload.expiresAt)
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
            DeviceConfig.shared.saveUserToken(payload.token, expiresAt: payload.expiresAt)
            isAuthenticated = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func loginWithApple(credential: ASAuthorizationAppleIDCredential) async {
        guard let tokenData = credential.identityToken,
              let identityToken = String(data: tokenData, encoding: .utf8),
              !identityToken.isEmpty else {
            errorMessage = "无法读取 Apple 身份令牌，请重试。"
            return
        }

        let fullName = formattedName(from: credential.fullName)
        await loginWithApple(
            identityToken: identityToken,
            userIdentifier: credential.user,
            email: credential.email,
            fullName: fullName
        )
    }

    func loginWithApple(identityToken: String, userIdentifier: String, email: String?, fullName: String?) async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }

        do {
            let payload = try await OpenClawAPI.shared.loginWithApple(
                identityToken: identityToken,
                userIdentifier: userIdentifier,
                email: email,
                fullName: fullName
            )
            DeviceConfig.shared.saveUserToken(payload.token, expiresAt: payload.expiresAt)
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

    private func formattedName(from components: PersonNameComponents?) -> String? {
        guard let components else { return nil }
        let formatted = PersonNameComponentsFormatter().string(from: components).trimmingCharacters(in: .whitespacesAndNewlines)
        return formatted.isEmpty ? nil : formatted
    }
}
