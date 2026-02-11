//
//  ChatView.swift
//  ClawPhones
//

import SwiftUI

struct ChatView: View {
    @StateObject private var viewModel: ChatViewModel
    @State private var inputText: String = ""

    init(conversationId: String?) {
        _viewModel = StateObject(wrappedValue: ChatViewModel(conversationId: conversationId))
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.messages) { message in
                            MessageRow(
                                message: message,
                                onRegenerate: message.role == .assistant ? {
                                    Task { await viewModel.regenerateAssistantMessage(messageId: message.id) }
                                } : nil,
                                onDelete: {
                                    viewModel.deleteMessage(messageId: message.id)
                                }
                            )
                                .id(message.id)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 12)
                }
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: viewModel.messages.count) { _ in
                    scrollToBottom(proxy, animated: true)
                }
                .onChange(of: viewModel.messages.last?.content ?? "") { _ in
                    scrollToBottom(proxy, animated: false)
                }
                .onAppear {
                    scrollToBottom(proxy, animated: false)
                }
            }

            Divider()

            ChatInputBar(text: $inputText, isLoading: viewModel.isLoading) {
                let textToSend = inputText
                inputText = ""
                Task {
                    await viewModel.sendMessage(text: textToSend)
                }
            }
        }
        .navigationTitle(viewModel.conversationTitle?.isEmpty == false ? (viewModel.conversationTitle ?? "") : "Chat")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if let cid = viewModel.conversationId {
                await viewModel.loadConversation(id: cid)
            } else {
                await viewModel.startNewConversation()
            }
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { newValue in
                if !newValue {
                    viewModel.errorMessage = nil
                }
            }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    private func scrollToBottom(_ proxy: ScrollViewProxy, animated: Bool) {
        guard let lastId = viewModel.messages.last?.id else { return }
        if animated {
            withAnimation(.easeOut(duration: 0.2)) {
                proxy.scrollTo(lastId, anchor: .bottom)
            }
        } else {
            proxy.scrollTo(lastId, anchor: .bottom)
        }
    }
}
