//
//  ChatViewModel.swift
//  ClawPhones
//

import SwiftUI

@MainActor
final class ChatViewModel: ObservableObject {
    @Published var messages: [Message] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var conversationTitle: String?

    private(set) var conversationId: String?

    static let defaultSystemPrompt = """
    你是 ClawPhones AI 助手，由 Oyster Labs 开发。你聪明、友好、高效。
    - 用用户的语言回复（中文问中文答，英文问英文答）
    - 回复简洁有用，避免冗长
    - 不要在回复中使用 Markdown 格式（不要用 ** 加粗、不要用 [] 链接）
    - 用纯文本回复，保持自然对话风格
    """

    init(conversationId: String? = nil) {
        self.conversationId = conversationId
    }

    func startNewConversation() async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }

        do {
            let conversation = try await OpenClawAPI.shared.createConversation(
                systemPrompt: Self.defaultSystemPrompt
            )
            conversationId = conversation.id
            conversationTitle = conversation.title ?? "New Chat"
            messages = []
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func loadConversation(id: String) async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }

        do {
            let detail = try await OpenClawAPI.shared.getConversation(id: id)
            conversationId = detail.id
            conversationTitle = detail.title ?? "Untitled"
            messages = detail.messages
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func sendMessage(text: String) async {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        errorMessage = nil

        if conversationId == nil {
            await startNewConversation()
        }

        guard let cid = conversationId else {
            return
        }

        let now = Int(Date().timeIntervalSince1970)
        let userMessage = Message(id: UUID().uuidString, role: .user, content: trimmed, createdAt: now)
        messages.append(userMessage)

        isLoading = true
        defer { isLoading = false }

        do {
            let response = try await OpenClawAPI.shared.chat(conversationId: cid, message: trimmed)
            let reply = Message(id: response.messageId, role: response.role, content: response.content, createdAt: response.createdAt)
            messages.append(reply)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
