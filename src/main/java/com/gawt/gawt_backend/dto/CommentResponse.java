package com.gawt.gawt_backend.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long itemId,
        Long userId,
        String username,
        String content,
        LocalDateTime createdAt) {
}
