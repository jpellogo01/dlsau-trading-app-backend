package com.gawt.gawt_backend.controller;

import com.gawt.gawt_backend.dto.CommentRequest;
import com.gawt.gawt_backend.dto.CommentResponse;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/items/{itemId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long itemId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User user) {
        CommentResponse response = commentService.createComment(itemId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User user) {
        CommentResponse response = commentService.updateComment(commentId, request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{itemId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByItem(
            @PathVariable Long itemId) {
        List<CommentResponse> responses = commentService.getCommentsByItemId(itemId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {
        commentService.deleteComment(commentId, user);
        return ResponseEntity.noContent().build();
    }
}
