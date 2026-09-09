package com.gawt.gawt_backend.controller;

import com.gawt.gawt_backend.dto.LikeResponse;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/items/{itemId}/likes")
    public ResponseEntity<LikeResponse> addLike(
            @PathVariable Long itemId,
            @AuthenticationPrincipal User user) {
        LikeResponse response = likeService.addLike(itemId, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}/likes")
    public ResponseEntity<LikeResponse> removeLike(
            @PathVariable Long itemId,
            @AuthenticationPrincipal User user) {
        LikeResponse response = likeService.removeLike(itemId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{itemId}/likes")
    public ResponseEntity<LikeResponse> getLikeSummary(
            @PathVariable Long itemId,
            @AuthenticationPrincipal User user) {
        LikeResponse response = likeService.getLikeSummary(itemId, user);
        return ResponseEntity.ok(response);
    }
}
