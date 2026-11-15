package com.gawt.gawt_backend.controller;

import com.gawt.gawt_backend.dto.*;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** "Message Owner" button: start a thread for an item, or return the existing one. */
    @PostMapping
    public ResponseEntity<ConversationResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.startOrGetConversation(request.itemId(), user));
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getMyConversations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getMyConversations(user));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", chatService.getUnreadCount(user)));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getConversation(conversationId, user));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getMessages(conversationId, user));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(conversationId, request.content(), user));
    }

    /** Send one photo (field "image") with an optional caption (field "content"). */
    @PostMapping(value = "/{conversationId}/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> sendImage(
            @PathVariable Long conversationId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "content", required = false) String content,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendImageMessage(conversationId, image, content, user));
    }

    /** Only the poster may switch the item between AVAILABLE and TRADED. */
    @PatchMapping("/{conversationId}/item-status")
    public ResponseEntity<ConversationResponse> updateItemStatus(
            @PathVariable Long conversationId,
            @Valid @RequestBody UpdateItemStatusRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.updateItemStatus(conversationId, request.status(), user));
    }

    /** Optional: send a message over the socket. Client sends to /app/chats/{id}/send. */
    @MessageMapping("/chats/{conversationId}/send")
    public void sendOverSocket(
            @DestinationVariable Long conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            chatService.sendMessage(conversationId, request.content(), user);
        }
    }
}
