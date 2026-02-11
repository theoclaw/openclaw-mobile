//
//  ConversationListViewModel.swift
//  ClawPhones
//

import SwiftUI

@MainActor
final class ConversationListViewModel: ObservableObject {
    @Published var conversations: [ConversationSummary] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    private let cache = ConversationCache.shared

    func loadConversations() async {
        errorMessage = nil

        let cached = await cache.loadConversations()
        if !cached.isEmpty {
            conversations = cached
        }

        isLoading = true
        defer { isLoading = false }

        do {
            let latest = try await OpenClawAPI.shared.listConversations(limit: 50, offset: 0)
            conversations = latest
            await cache.saveConversations(latest)
        } catch {
            if conversations.isEmpty {
                errorMessage = error.localizedDescription
            }
        }
    }

    func deleteConversation(id: String) async {
        errorMessage = nil

        do {
            let deleted = try await OpenClawAPI.shared.deleteConversation(id: id)
            if deleted {
                conversations.removeAll { $0.id == id }
                await cache.removeConversation(id: id)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
