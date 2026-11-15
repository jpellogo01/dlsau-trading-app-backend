package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.Status;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long itemId,
        String itemTitle,
        String itemThumbnailUrl,
        Status itemStatus,
        Long tradedWithUserId,
        Long otherUserId,
        String otherUsername,
        boolean isPoster,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        long unreadCount,
        /** Photos already sent in this conversation (both users combined). */
        long imagesUsed,
        /** Maximum photos allowed in this conversation. */
        int imagesLimit,
        LocalDateTime createdAt) {
}
