//
//  AIConfigView.swift
//  ClawPhones
//

import SwiftUI

struct AIConfigView: View {
    @ObservedObject var viewModel: SettingsViewModel

    @State private var persona: SettingsViewModel.Persona
    @State private var customPrompt: String
    @State private var temperature: Double

    @State private var showErrorAlert: Bool = false
    @State private var alertMessage: String = ""
    @State private var isSaving: Bool = false

    init(viewModel: SettingsViewModel) {
        self.viewModel = viewModel
        _persona = State(initialValue: viewModel.aiConfig.persona)
        _customPrompt = State(initialValue: viewModel.aiConfig.customPrompt)
        _temperature = State(initialValue: viewModel.aiConfig.temperature)
    }

    var body: some View {
        Form { formContent }
        .navigationTitle("AI 人设")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("保存") {
                    save()
                }
                .disabled(isSaving)
            }
        }
        .overlay {
            if isSaving {
                ProgressView()
            }
        }
        .alert("错误", isPresented: $showErrorAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(alertMessage)
        }
        .onAppear {
            // Keep UI in sync if settings loaded after navigation.
            persona = viewModel.aiConfig.persona
            customPrompt = viewModel.aiConfig.customPrompt
            temperature = viewModel.aiConfig.temperature
        }
    }

    @ViewBuilder
    private var formContent: some View {
        Section("人设") {
            ForEach(SettingsViewModel.Persona.allCases) { item in
                Button {
                    persona = item
                } label: {
                    HStack {
                        Text(item.displayName)
                        Spacer()
                        if persona == item {
                            Image(systemName: "checkmark")
                                .foregroundStyle(.tint)
                        }
                    }
                }
            }
        }

        if persona == .custom {
            Section {
                TextEditor(text: $customPrompt)
                    .frame(minHeight: 120)
                    .onChange(of: customPrompt) { _, newValue in
                        if newValue.count > 500 {
                            customPrompt = String(newValue.prefix(500))
                        }
                    }

                HStack {
                    Spacer()
                    Text("\(customPrompt.count)/500")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            } header: {
                Text("自定义提示词")
            } footer: {
                Text("最多 500 字。")
                    .font(.footnote)
            }
        }

        Section("回复风格") {
            HStack {
                Text("简洁")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Slider(value: $temperature, in: 0.3...1.0, step: 0.05)
                Text("详细")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Text("当前：\(String(format: "%.2f", temperature))")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private func save() {
        isSaving = true
        Task {
            defer { isSaving = false }

            let trimmed = customPrompt.trimmingCharacters(in: .whitespacesAndNewlines)
            await viewModel.updateAIConfig(
                persona: persona,
                customPrompt: persona == .custom ? trimmed : nil,
                temperature: temperature
            )

            if let err = viewModel.errorMessage, !err.isEmpty {
                alertMessage = err
                showErrorAlert = true
            }
        }
    }
}
