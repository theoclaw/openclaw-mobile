//
//  MessageRow.swift
//  ClawPhones
//

import SwiftUI
import UIKit

struct MessageRow: View {
    let message: Message
    let onRegenerate: (() -> Void)?
    let onDelete: () -> Void

    private var isUser: Bool {
        message.role == .user
    }

    private var isAssistant: Bool {
        message.role == .assistant
    }

    var body: some View {
        HStack(alignment: .bottom) {
            if isUser {
                Spacer(minLength: 50)
            }

            contentView
                .font(.body)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(isUser ? Color.accentColor : Color(uiColor: .secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .frame(maxWidth: .infinity, alignment: isUser ? .trailing : .leading)
                .contextMenu {
                    Button("复制") {
                        UIPasteboard.general.string = message.content
                    }

                    if isAssistant, let onRegenerate {
                        Button("重新生成") {
                            onRegenerate()
                        }
                    }

                    Button("删除", role: .destructive) {
                        onDelete()
                    }
                }

            if !isUser {
                Spacer(minLength: 50)
            }
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var contentView: some View {
        if isUser {
            Text(message.content)
                .foregroundStyle(Color.white)
        } else {
            if message.content.isEmpty {
                ThinkingIndicator()
                    .padding(.vertical, 2)
            } else if let attributed = try? AttributedString(
                markdown: message.content,
                options: .init(interpretedSyntax: .full)
            ) {
                Text(attributed)
                    .foregroundStyle(Color.primary)
                    .tint(Color.accentColor)
            } else {
                Text(message.content)
                    .foregroundStyle(Color.primary)
            }
        }
    }
}
