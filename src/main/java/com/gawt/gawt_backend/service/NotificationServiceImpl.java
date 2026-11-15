package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.NotificationResponse;
import com.gawt.gawt_backend.model.Comment;
import com.gawt.gawt_backend.model.Notification;
import com.gawt.gawt_backend.model.NotificationType;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    /** Users receive live notifications at /user/queue/notifications. */
    private static final String USER_QUEUE = "/queue/notifications";
    private static final int PREVIEW_LENGTH = 120;
    private static final int LIST_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notify(User recipient, User actor, NotificationType type, Comment comment) {
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return; // never notify people about their own actions
        }
        String text = comment.getContent();
        Notification saved = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .item(comment.getItem())
                .comment(comment)
                .preview(text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH) + "…" : text)
                .build());

        NotificationResponse response = toResponse(saved);
        String username = recipient.getUsername();
        Runnable push = () -> messagingTemplate.convertAndSendToUser(username, USER_QUEUE, response);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Only push once the comment and notification are really saved.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(User user) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, LIST_LIMIT))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Override
    public void markRead(Long notificationId, User user) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getRecipient().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setIsRead(true);
    }

    @Override
    public void markAllRead(User user) {
        notificationRepository.markAllRead(user.getId());
    }

    @Override
    public void deleteForComments(Collection<Long> commentIds) {
        if (!commentIds.isEmpty()) {
            notificationRepository.deleteByCommentIds(commentIds);
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getActor().getId(),
                n.getActor().getUsername(),
                n.getItem() != null ? n.getItem().getId() : null,
                n.getItem() != null ? n.getItem().getTitle() : null,
                n.getComment() != null ? n.getComment().getId() : null,
                n.getPreview(),
                Boolean.TRUE.equals(n.getIsRead()),
                n.getCreatedAt());
    }
}
