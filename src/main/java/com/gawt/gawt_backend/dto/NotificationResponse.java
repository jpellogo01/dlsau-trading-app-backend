package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        Long actorId,
        String actorUsername,
        Long itemId,
        String itemTitle,
        /** The comment/reply that triggered it (used to jump to it). */
        Long commentId,
        String preview,
        boolean isRead,
        LocalDateTime createdAt) {
}
