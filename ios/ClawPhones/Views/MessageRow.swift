//
//  MessageRow.swift
//  ClawPhones
//

import SwiftUI
import UIKit

struct MessageRow: View {
    let message: Message
    let timestampText: String?
    let onRetry: (() -> Void)?
    let onCopy: (() -> Void)?
    let onRegenerate: (() -> Void)?
    let onDelete: () -> Void

    private var isUser: Bool {
        message.role == .user
    }

    private var isAssistant: Bool {
        message.role == .assistant
    }

    private enum ContentSegment {
        case markdown(String)
        case code(language: String?, code: String)
    }

    private static let swiftKeywords: Set<String> = [
        "as", "associatedtype", "async", "await", "break", "case", "catch", "class",
        "continue", "default", "defer", "do", "else", "enum", "extension", "false",
        "for", "func", "guard", "if", "import", "in", "init", "inout", "is", "let",
        "nil", "operator", "private", "protocol", "public", "repeat", "return", "self",
        "static", "struct", "subscript", "switch", "throw", "throws", "true", "try",
        "typealias", "var", "where", "while"
    ]

    private static let pythonKeywords: Set<String> = [
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "False", "finally", "for", "from", "global",
        "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass",
        "raise", "return", "True", "try", "while", "with", "yield"
    ]

    private static let javascriptKeywords: Set<String> = [
        "await", "break", "case", "catch", "class", "const", "continue", "debugger",
        "default", "delete", "do", "else", "export", "extends", "false", "finally",
        "for", "function", "if", "import", "in", "instanceof", "let", "new", "null",
        "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var",
        "while", "with", "yield"
    ]

    var body: some View {
        HStack(alignment: .bottom) {
            if isUser {
                Spacer(minLength: 50)
            }

            VStack(alignment: .trailing, spacing: 0) {
                contentView
                    .font(.body)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(isUser ? Color.accentColor : Color(uiColor: .secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .contextMenu {
                        Button("复制") {
                            UIPasteboard.general.string = message.content
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            onCopy?()
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

                if let timestampText {
                    Text(timestampText)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.top, 2)
                }

                if isUser, message.deliveryState != .sent {
                    HStack(spacing: 8) {
                        Text(message.deliveryState == .failed ? "发送失败" : "发送中...")
                            .font(.caption2)
                            .foregroundStyle(message.deliveryState == .failed ? Color.red.opacity(0.9) : .secondary)

                        if message.deliveryState == .failed, let onRetry {
                            Button("重试", action: onRetry)
                                .font(.caption2.weight(.semibold))
                                .buttonStyle(.plain)
                                .foregroundStyle(Color.accentColor)
                        }
                    }
                    .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: isUser ? .trailing : .leading)

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
                .foregroundStyle(message.deliveryState == .sending ? Color(white: 0.78) : Color.white)
        } else {
            assistantContentView
        }
    }

    @ViewBuilder
    private var assistantContentView: some View {
        if message.content.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                ThinkingIndicator()
                Text("正在思考...")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 2)
        } else {
            let segments = parseContentSegments(from: message.content)

            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(segments.enumerated()), id: \.offset) { _, segment in
                    segmentView(segment)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    @ViewBuilder
    private func segmentView(_ segment: ContentSegment) -> some View {
        switch segment {
        case let .markdown(text):
            markdownTextView(text)
        case let .code(language, code):
            codeBlockView(language: language, code: code)
        }
    }

    @ViewBuilder
    private func markdownTextView(_ markdown: String) -> some View {
        if let attributed = try? AttributedString(
            markdown: markdown,
            options: .init(interpretedSyntax: .full)
        ) {
            Text(attributed)
                .foregroundStyle(Color.primary)
                .tint(Color.accentColor)
        } else {
            Text(markdown)
                .foregroundStyle(Color.primary)
        }
    }

    private func codeBlockView(language: String?, code: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if let tag = normalizedLanguageTag(language) {
                Text(tag.uppercased())
                    .font(.caption2)
                    .foregroundStyle(Color.white.opacity(0.7))
            }

            ScrollView(.horizontal, showsIndicators: true) {
                highlightedCodeText(code: code, language: language)
                    .font(.system(.body, design: .monospaced))
                    .textSelection(.enabled)
                    .fixedSize(horizontal: true, vertical: true)
            }
        }
        .padding(10)
        .background(Color(red: 0.08, green: 0.09, blue: 0.12))
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func parseContentSegments(from markdown: String) -> [ContentSegment] {
        let pattern = "```([A-Za-z0-9_+-]*)[ \\t]*\\n([\\s\\S]*?)```"
        guard let regex = try? NSRegularExpression(pattern: pattern) else {
            return [.markdown(markdown)]
        }

        let range = NSRange(markdown.startIndex..<markdown.endIndex, in: markdown)
        let matches = regex.matches(in: markdown, options: [], range: range)
        guard !matches.isEmpty else {
            return [.markdown(markdown)]
        }

        var segments: [ContentSegment] = []
        var cursor = markdown.startIndex

        for match in matches {
            guard let fullRange = Range(match.range, in: markdown) else { continue }

            if cursor < fullRange.lowerBound {
                segments.append(.markdown(String(markdown[cursor..<fullRange.lowerBound])))
            }

            let language: String? = {
                guard let languageRange = Range(match.range(at: 1), in: markdown) else { return nil }
                let value = String(markdown[languageRange]).trimmingCharacters(in: .whitespacesAndNewlines)
                return value.isEmpty ? nil : value.lowercased()
            }()

            var code = ""
            if let codeRange = Range(match.range(at: 2), in: markdown) {
                code = String(markdown[codeRange]).replacingOccurrences(of: "\r\n", with: "\n")
            }

            if code.hasSuffix("\n") {
                code.removeLast()
            }

            segments.append(.code(language: language, code: code))
            cursor = fullRange.upperBound
        }

        if cursor < markdown.endIndex {
            segments.append(.markdown(String(markdown[cursor..<markdown.endIndex])))
        }

        return segments
    }

    private func highlightedCodeText(code: String, language: String?) -> Text {
        let normalizedCode = code.replacingOccurrences(of: "\r\n", with: "\n")
        let lines = normalizedCode.split(separator: "\n", omittingEmptySubsequences: false)
        let keywordSet = keywordSet(for: language)

        var output = Text("")
        for (index, line) in lines.enumerated() {
            output = output + highlightedLine(String(line), language: language, keywords: keywordSet)
            if index < lines.count - 1 {
                output = output + Text("\n")
            }
        }

        return output
    }

    private func highlightedLine(_ line: String, language: String?, keywords: Set<String>) -> Text {
        let (codePart, commentPart) = splitComment(from: line, language: language)
        var output = tokenizeCode(codePart, keywords: keywords)

        if let commentPart, !commentPart.isEmpty {
            output = output + Text(commentPart).foregroundColor(Color(red: 0.45, green: 0.74, blue: 0.5))
        }

        return output
    }

    private func splitComment(from line: String, language: String?) -> (String, String?) {
        let normalized = normalizedLanguage(language)
        guard normalized == "python" || normalized == "swift" || normalized == "javascript" else {
            return (line, nil)
        }

        var index = line.startIndex
        var inSingleQuote = false
        var inDoubleQuote = false
        var isEscaped = false

        while index < line.endIndex {
            let character = line[index]

            if isEscaped {
                isEscaped = false
            } else if character == "\\" {
                isEscaped = inSingleQuote || inDoubleQuote
            } else if character == "\"" && !inSingleQuote {
                inDoubleQuote.toggle()
            } else if character == "'" && !inDoubleQuote {
                inSingleQuote.toggle()
            } else if !inSingleQuote && !inDoubleQuote {
                if normalized == "python", character == "#" {
                    return (String(line[..<index]), String(line[index...]))
                }

                if normalized == "swift" || normalized == "javascript" {
                    let nextIndex = line.index(after: index)
                    if character == "/", nextIndex < line.endIndex, line[nextIndex] == "/" {
                        return (String(line[..<index]), String(line[index...]))
                    }
                }
            }

            index = line.index(after: index)
        }

        return (line, nil)
    }

    private func tokenizeCode(_ text: String, keywords: Set<String>) -> Text {
        let pattern = "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\\b[A-Za-z_][A-Za-z0-9_]*\\b"
        guard let regex = try? NSRegularExpression(pattern: pattern) else {
            return Text(text).foregroundColor(.white)
        }

        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        let matches = regex.matches(in: text, options: [], range: range)
        var cursor = text.startIndex
        var output = Text("")

        for match in matches {
            guard let tokenRange = Range(match.range, in: text) else { continue }

            if cursor < tokenRange.lowerBound {
                let plain = String(text[cursor..<tokenRange.lowerBound])
                output = output + Text(plain).foregroundColor(Color.white.opacity(0.92))
            }

            let token = String(text[tokenRange])
            if token.hasPrefix("\"") || token.hasPrefix("'") {
                output = output + Text(token).foregroundColor(Color(red: 0.96, green: 0.69, blue: 0.32))
            } else if keywords.contains(token) {
                output = output + Text(token).foregroundColor(Color(red: 0.45, green: 0.78, blue: 1.0))
            } else {
                output = output + Text(token).foregroundColor(Color.white.opacity(0.92))
            }

            cursor = tokenRange.upperBound
        }

        if cursor < text.endIndex {
            output = output + Text(String(text[cursor...])).foregroundColor(Color.white.opacity(0.92))
        }

        return output
    }

    private func normalizedLanguage(_ language: String?) -> String {
        guard let language else { return "plain" }
        switch language.lowercased() {
        case "swift":
            return "swift"
        case "py", "python":
            return "python"
        case "js", "javascript", "ts", "typescript", "tsx":
            return "javascript"
        default:
            return "plain"
        }
    }

    private func normalizedLanguageTag(_ language: String?) -> String? {
        let normalized = normalizedLanguage(language)
        return normalized == "plain" ? nil : normalized
    }

    private func keywordSet(for language: String?) -> Set<String> {
        switch normalizedLanguage(language) {
        case "swift":
            return Self.swiftKeywords
        case "python":
            return Self.pythonKeywords
        case "javascript":
            return Self.javascriptKeywords
        default:
            return Self.swiftKeywords.union(Self.pythonKeywords).union(Self.javascriptKeywords)
        }
    }
}
