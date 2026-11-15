package com.gawt.gawt_backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param content  comment text
 * @param parentId optional: id of the comment being replied to (top-level or reply)
 */
public record CommentRequest(
        @NotBlank(message = "Comment content is required") String content,
        Long parentId) {
}
