package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.MessageType;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderUsername,
        String content,
        String imageUrl,
        MessageType type,
        boolean isRead,
        LocalDateTime createdAt) {
}
