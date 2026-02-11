//
//  Message.swift
//  ClawPhones
//
//  Data models for OpenClaw conversations and messages
//

import Foundation

struct Message: Identifiable, Codable, Hashable {
    let id: String
    let role: Role
    let content: String
    let createdAt: Int
    var deliveryState: DeliveryState = .sent
    var localQueueId: String? = nil
    var retryCount: Int = 0

    enum Role: String, Codable {
        case user
        case assistant
        case system
    }

    enum DeliveryState: String, Codable {
        case sent
        case sending
        case failed
    }

    enum CodingKeys: String, CodingKey {
        case id
        case role
        case content
        case createdAt = "created_at"
    }

    init(
        id: String,
        role: Role,
        content: String,
        createdAt: Int,
        deliveryState: DeliveryState = .sent,
        localQueueId: String? = nil,
        retryCount: Int = 0
    ) {
        self.id = id
        self.role = role
        self.content = content
        self.createdAt = createdAt
        self.deliveryState = deliveryState
        self.localQueueId = localQueueId
        self.retryCount = retryCount
    }
}

struct Conversation: Identifiable, Codable, Hashable {
    let id: String
    let title: String?
    let createdAt: Int

    enum CodingKeys: String, CodingKey {
        case id
        case title
        case createdAt = "created_at"
    }
}

struct ConversationSummary: Identifiable, Codable, Hashable {
    let id: String
    let title: String?
    let lastMessage: String?
    let createdAt: Int
    let updatedAt: Int?
    let messageCount: Int

    enum CodingKeys: String, CodingKey {
        case id
        case title
        case lastMessage = "last_message"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case messageCount = "message_count"
    }
}

struct ConversationDetail: Identifiable, Codable, Hashable {
    let id: String
    let title: String?
    let createdAt: Int
    let messages: [Message]

    enum CodingKeys: String, CodingKey {
        case id
        case title
        case createdAt = "created_at"
        case messages
    }
}

struct ChatMessageResponse: Codable, Hashable {
    let messageId: String
    let role: Message.Role
    let content: String
    let conversationId: String
    let createdAt: Int

    enum CodingKeys: String, CodingKey {
        case messageId = "message_id"
        case role
        case content
        case conversationId = "conversation_id"
        case createdAt = "created_at"
    }
}

struct ConversationListResponse: Codable {
    let conversations: [ConversationSummary]
}

struct DeleteConversationResponse: Codable {
    let deleted: Bool
}

// MARK: - Error Models

enum ClawPhonesError: LocalizedError {
    case noDeviceToken
    case networkError(Error)
    case apiError(String)
    case decodingError
    case unauthorized

    var errorDescription: String? {
        switch self {
        case .noDeviceToken:
            return "Device token not found. Please enter your token."
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .apiError(let message):
            return message
        case .decodingError:
            return "Failed to parse server response"
        case .unauthorized:
            return "Authentication failed. Please check your token."
        }
    }
}
