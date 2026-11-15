package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.CommentRequest;
import com.gawt.gawt_backend.dto.CommentResponse;
import com.gawt.gawt_backend.model.Comment;
import com.gawt.gawt_backend.model.Item;
import com.gawt.gawt_backend.model.NotificationType;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.repository.CommentRepository;
import com.gawt.gawt_backend.repository.ItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;

    @Override
    public CommentResponse createComment(Long itemId, CommentRequest request, User user) {
        Item item = getItemOrThrow(itemId);

        Comment parent = null;      // top-level comment of the thread
        Comment repliedTo = null;   // the exact comment the user clicked "Reply" on
        if (request.parentId() != null) {
            repliedTo = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
            if (!repliedTo.getItem().getId().equals(itemId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That comment belongs to another item");
            }
            // Facebook-style: threads are 2 levels deep; a reply to a reply joins the same thread.
            parent = repliedTo.getParent() != null ? repliedTo.getParent() : repliedTo;
        }

        Comment saved = commentRepository.save(Comment.builder()
                .content(request.content().trim())
                .item(item)
                .user(user)
                .parent(parent)
                .replyToUser(repliedTo != null ? repliedTo.getUser() : null)
                .build());

        sendNotifications(saved, item, parent, repliedTo, user);
        return toResponse(saved);
    }

    /**
     * Who gets notified (never the author themselves, never twice):
     * - the person whose comment was replied to   -> COMMENT_REPLY
     * - the author of the thread's top comment    -> COMMENT_REPLY
     * - the item owner (if not already notified)  -> ITEM_COMMENT
     */
    private void sendNotifications(Comment saved, Item item, Comment parent, Comment repliedTo, User author) {
        Map<Long, NotificationType> recipients = new LinkedHashMap<>();
        Map<Long, User> users = new LinkedHashMap<>();

        if (repliedTo != null) {
            addRecipient(recipients, users, repliedTo.getUser(), NotificationType.COMMENT_REPLY);
            addRecipient(recipients, users, parent.getUser(), NotificationType.COMMENT_REPLY);
        }
        addRecipient(recipients, users, item.getUser(), NotificationType.ITEM_COMMENT);

        recipients.forEach((userId, type) -> notificationService.notify(users.get(userId), author, type, saved));
    }

    private void addRecipient(Map<Long, NotificationType> recipients, Map<Long, User> users, User u,
            NotificationType type) {
        if (u != null && !recipients.containsKey(u.getId())) {
            recipients.put(u.getId(), type);
            users.put(u.getId(), u);
        }
    }

    @Override
    public CommentResponse updateComment(Long commentId, CommentRequest request, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean isOwner = comment.getUser().getId().equals(user.getId());
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own comments");
        }

        comment.setContent(request.content().trim());
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

        // Deleting a top-level comment removes its replies too (like Facebook).
        List<Comment> replies = commentRepository.findByParentId(commentId);
        List<Long> ids = new ArrayList<>();
        ids.add(commentId);
        replies.forEach(r -> ids.add(r.getId()));

        notificationService.deleteForComments(ids);
        commentRepository.deleteAll(replies);
        commentRepository.delete(comment);
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    private CommentResponse toResponse(Comment comment) {
        User replyTo = comment.getReplyToUser();
        return new CommentResponse(
                comment.getId(),
                comment.getItem().getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                replyTo != null ? replyTo.getId() : null,
                replyTo != null ? replyTo.getUsername() : null);
    }
}
