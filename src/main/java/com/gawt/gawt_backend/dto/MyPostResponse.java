package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.Condition;
import com.gawt.gawt_backend.model.Status;

import java.time.LocalDateTime;

/** One of the logged-in user's own posts, with activity counts for the "My Posts" tab. */
public record MyPostResponse(
        Long id,
        String title,
        String description,
        String category,
        Condition condition,
        String meetupLocation,
        String thumbnailUrl,
        Status status,
        Long tradedWithUserId,
        String tradedWithUsername,
        LocalDateTime tradedAt,
        LocalDateTime createdAt,
        long commentCount,
        long likeCount,
        long chatCount) {
}
