package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.CommentRequest;
import com.gawt.gawt_backend.dto.CommentResponse;
import com.gawt.gawt_backend.model.User;

import java.util.List;

public interface CommentService {
    CommentResponse createComment(Long itemId, CommentRequest request, User user);

    CommentResponse updateComment(Long commentId, CommentRequest request, User user);

    List<CommentResponse> getCommentsByItemId(Long itemId);

    void deleteComment(Long commentId, User user);
}
