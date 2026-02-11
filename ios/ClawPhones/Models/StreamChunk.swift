//
//  StreamChunk.swift
//  ClawPhones
//
//  Incremental streaming chunks from SSE chat endpoint.
//

import Foundation

struct StreamChunk {
    let delta: String
    let done: Bool
    let messageId: String?
    let fullContent: String?
}

