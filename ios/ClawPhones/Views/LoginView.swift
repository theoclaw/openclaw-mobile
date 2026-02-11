//
//  LoginView.swift
//  ClawPhones
//

import SwiftUI
import AuthenticationServices
import UIKit

struct LoginView: View {
    @EnvironmentObject private var auth: AuthViewModel

    private enum Mode: String, CaseIterable, Identifiable {
        case login = "登录"
        case register = "注册"

        var id: String { rawValue }
    }

    @State private var mode: Mode = .login
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var name: String = ""
    @State private var localErrorMessage: String?
    @StateObject private var appleSignIn = AppleSignInCoordinator()

    var body: some View {
        Form {
            Section {
                Picker("模式", selection: $mode) {
                    ForEach(Mode.allCases) { item in
                        Text(item.rawValue).tag(item)
                    }
                }
                .pickerStyle(.segmented)
            }

            Section(mode == .login ? "登录" : "注册") {
                TextField("邮箱", text: $email)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled(true)

                SecureField("密码 (至少 8 位)", text: $password)
                    .textInputAutocapitalization(.never)

                if mode == .register {
                    TextField("昵称 (可选)", text: $name)
                        .textInputAutocapitalization(.words)
                }

                if !email.isEmpty && !isValidEmail(email) {
                    Text("邮箱格式不正确")
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                if !password.isEmpty && password.count < 8 {
                    Text("密码至少 8 位")
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            }

            Section {
                Button {
                    submit()
                } label: {
                    HStack {
                        Spacer()
                        if auth.isLoading {
                            ProgressView()
                        } else {
                            Text(mode == .login ? "登录" : "注册")
                                .fontWeight(.semibold)
                        }
                        Spacer()
                    }
                }
                .disabled(!canSubmit || auth.isLoading)
            }

            Section {
                AppleIDSignInButton {
                    startAppleSignIn()
                }
                .frame(height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .disabled(auth.isLoading)
                .accessibilityLabel("Sign in with Apple")
            } footer: {
                VStack(spacing: 10) {
                    Text("使用 Apple 账号一键登录。")
                        .font(.footnote)

                    NavigationLink {
                        SetupView {
                            auth.refreshAuthState()
                        }
                    } label: {
                        Text("开发者? 直接输入 Token")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                }
            }
        }
        .navigationTitle("ClawPhones")
        .alert("错误", isPresented: Binding(
            get: { mergedErrorMessage != nil },
            set: { newValue in
                if !newValue {
                    localErrorMessage = nil
                    auth.errorMessage = nil
                }
            }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(mergedErrorMessage ?? "")
        }
    }

    private var mergedErrorMessage: String? {
        localErrorMessage ?? auth.errorMessage
    }

    private var canSubmit: Bool {
        isValidEmail(email) && password.count >= 8
    }

    private func submit() {
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        guard isValidEmail(trimmedEmail) else {
            localErrorMessage = "请输入正确的邮箱。"
            return
        }
        guard trimmedPassword.count >= 8 else {
            localErrorMessage = "密码至少 8 位。"
            return
        }

        Task {
            switch mode {
            case .login:
                await auth.login(email: trimmedEmail, password: trimmedPassword)
            case .register:
                let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
                await auth.register(email: trimmedEmail, password: trimmedPassword, name: trimmedName.isEmpty ? nil : trimmedName)
            }
        }
    }

    private func startAppleSignIn() {
        appleSignIn.signIn { result in
            switch result {
            case .success(let credential):
                Task {
                    await auth.loginWithApple(credential: credential)
                }
            case .failure(let error):
                if let authError = error as? ASAuthorizationError, authError.code == .canceled {
                    return
                }
                DispatchQueue.main.async {
                    localErrorMessage = error.localizedDescription
                }
            }
        }
    }

    private func isValidEmail(_ email: String) -> Bool {
        let pattern = #"^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
        return email.range(of: pattern, options: .regularExpression) != nil
    }
}

private struct AppleIDSignInButton: UIViewRepresentable {
    let action: () -> Void

    func makeUIView(context: Context) -> ASAuthorizationAppleIDButton {
        let button = ASAuthorizationAppleIDButton(type: .signIn, style: .black)
        button.cornerRadius = 12
        button.addTarget(context.coordinator, action: #selector(Coordinator.tap), for: .touchUpInside)
        button.isEnabled = context.environment.isEnabled
        button.alpha = context.environment.isEnabled ? 1.0 : 0.55
        return button
    }

    func updateUIView(_ uiView: ASAuthorizationAppleIDButton, context: Context) {
        uiView.isEnabled = context.environment.isEnabled
        uiView.alpha = context.environment.isEnabled ? 1.0 : 0.55
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(action: action)
    }

    final class Coordinator: NSObject {
        let action: () -> Void

        init(action: @escaping () -> Void) {
            self.action = action
        }

        @objc func tap() {
            action()
        }
    }
}

private final class AppleSignInCoordinator: NSObject, ObservableObject {
    private var completion: ((Result<ASAuthorizationAppleIDCredential, Error>) -> Void)?

    func signIn(completion: @escaping (Result<ASAuthorizationAppleIDCredential, Error>) -> Void) {
        self.completion = completion
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }
}

extension AppleSignInCoordinator: ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        guard let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
              let window = scene.windows.first(where: { $0.isKeyWindow }) else {
            return UIWindow()
        }
        return window
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            completion?(.failure(NSError(domain: "AppleSignIn", code: -1, userInfo: [NSLocalizedDescriptionKey: "Apple credential missing."])))
            completion = nil
            return
        }
        completion?(.success(credential))
        completion = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        completion?(.failure(error))
        completion = nil
    }
}
