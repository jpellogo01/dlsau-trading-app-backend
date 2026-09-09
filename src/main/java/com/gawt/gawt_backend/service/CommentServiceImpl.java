package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.CommentRequest;
import com.gawt.gawt_backend.dto.CommentResponse;
import com.gawt.gawt_backend.model.Comment;
import com.gawt.gawt_backend.model.Item;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.repository.CommentRepository;
import com.gawt.gawt_backend.repository.ItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ItemRepository itemRepository;

    @Override
    public CommentResponse createComment(Long itemId, CommentRequest request, User user) {
        Item item = getItemOrThrow(itemId);

        Comment comment = Comment.builder()
                .content(request.content())
                .item(item)
                .user(user)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return toResponse(savedComment);
    }

    @Override
    public CommentResponse updateComment(Long commentId, CommentRequest request, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean isOwner = comment.getUser().getId().equals(user.getId());
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own comments");
        }

        comment.setContent(request.content());
        Comment updatedComment = commentRepository.save(comment);
        return toResponse(updatedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByItemId(Long itemId) {
        getItemOrThrow(itemId);
        return commentRepository.findByItemIdOrderByCreatedAtDesc(itemId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean isOwner = comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getItem().getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
