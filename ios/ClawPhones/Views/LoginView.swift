//
//  LoginView.swift
//  ClawPhones
//

import SwiftUI

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
    @State private var showAppleComingSoonAlert: Bool = false

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
                Button {
                    showAppleComingSoonAlert = true
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "applelogo")
                        Text("Sign in with Apple (Coming Soon)")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .foregroundStyle(Color(.systemGray))
                    .background(Color(.systemGray5))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Sign in with Apple (Coming Soon)")
                .alert("This feature is coming soon", isPresented: $showAppleComingSoonAlert) {
                    Button("OK", role: .cancel) {}
                }
            } footer: {
                VStack(spacing: 10) {
                    Text("Apple 登录功能即将上线。")
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

    private func isValidEmail(_ email: String) -> Bool {
        let pattern = #"^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
        return email.range(of: pattern, options: .regularExpression) != nil
    }
}
