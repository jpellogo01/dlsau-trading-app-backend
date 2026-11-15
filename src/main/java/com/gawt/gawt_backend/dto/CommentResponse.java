package com.gawt.gawt_backend.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long itemId,
        Long userId,
        String username,
        String content,
        LocalDateTime createdAt,
        /** Top-level comment this reply belongs to; null for top-level comments. */
        Long parentId,
        /** Person being replied to (for the @username tag); null for top-level comments. */
        Long replyToUserId,
        String replyToUsername) {
}
