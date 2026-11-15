package com.gawt.gawt_backend.controller;

import com.gawt.gawt_backend.dto.NotificationResponse;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Latest 50 notifications, newest first. */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(user));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long notificationId, @AuthenticationPrincipal User user) {
        notificationService.markRead(notificationId, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(user);
        return ResponseEntity.noContent().build();
    }
}
