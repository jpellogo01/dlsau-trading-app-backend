package com.gawt.gawt_backend.dto;

public record LikeResponse(
        Long itemId,
        long totalLikes,
        boolean likedByCurrentUser) {
}
