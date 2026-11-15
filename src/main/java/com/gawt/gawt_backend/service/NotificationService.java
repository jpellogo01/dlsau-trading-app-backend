package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.NotificationResponse;
import com.gawt.gawt_backend.model.Comment;
import com.gawt.gawt_backend.model.NotificationType;
import com.gawt.gawt_backend.model.User;

import java.util.Collection;
import java.util.List;

public interface NotificationService {

    /** Creates a notification and pushes it live. Does nothing if recipient == actor. */
    void notify(User recipient, User actor, NotificationType type, Comment comment);

    List<NotificationResponse> getMyNotifications(User user);

    long getUnreadCount(User user);

    void markRead(Long notificationId, User user);

    void markAllRead(User user);

    /** Removes notifications that point at comments being deleted. */
    void deleteForComments(Collection<Long> commentIds);
}
