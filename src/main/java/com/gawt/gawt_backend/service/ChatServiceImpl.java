package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.ChatEvent;
import com.gawt.gawt_backend.dto.ConversationResponse;
import com.gawt.gawt_backend.dto.ItemStatusEvent;
import com.gawt.gawt_backend.dto.MessageResponse;
import com.gawt.gawt_backend.model.*;
import com.gawt.gawt_backend.repository.ConversationRepository;
import com.gawt.gawt_backend.repository.ItemRepository;
import com.gawt.gawt_backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final String USER_QUEUE = "/queue/chat";
    private static final int PREVIEW_LENGTH = 120;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ItemRepository itemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CloudinaryService cloudinaryService;

    /** Photos allowed per conversation (both users combined). */
    @Value("${chat.max-images-per-conversation:10}")
    private int maxImagesPerConversation;

    @Override
    public ConversationResponse startOrGetConversation(Long itemId, User user) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (item.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This item has no owner");
        }
        if (item.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot message yourself about your own item");
        }

        Conversation conversation = conversationRepository.findByItemIdAndTraderId(itemId, user.getId())
                .orElseGet(() -> {
                    if (item.getStatus() == Status.TRADED) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "This item has already been traded");
                    }
                    return conversationRepository.save(Conversation.builder()
                            .item(item)
                            .poster(item.getUser())
                            .trader(user)
                            .build());
                });

        return toResponse(conversation, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(User user) {
        return conversationRepository.findAllForUser(user.getId())
                .stream()
                .map(c -> toResponse(c, user))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long conversationId, User user) {
        return toResponse(getParticipantConversation(conversationId, user), user);
    }

    @Override
    public List<MessageResponse> getMessages(Long conversationId, User user) {
        getParticipantConversation(conversationId, user);
        messageRepository.markAsRead(conversationId, user.getId());
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MessageResponse sendMessage(Long conversationId, String content, User user) {
        Conversation conversation = getParticipantConversation(conversationId, user);
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        Message saved = saveMessage(conversation, user, trimmed, null, MessageType.TEXT);
        MessageResponse response = toResponse(saved);
        ChatEvent event = ChatEvent.message(response);
        pushAfterCommit(Set.of(conversation.getPoster().getUsername(), conversation.getTrader().getUsername()), event);
        return response;
    }

    @Override
    public MessageResponse sendImageMessage(Long conversationId, MultipartFile image, String caption, User user) {
        Conversation conversation = getParticipantConversation(conversationId, user);
        cloudinaryService.validateImage(image);

        long used = messageRepository.countByConversationIdAndImageUrlIsNotNull(conversationId);
        if (used >= maxImagesPerConversation) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Photo limit reached: only " + maxImagesPerConversation + " photos are allowed per conversation");
        }

        String imageUrl = cloudinaryService.uploadImage(image, "gawt/chats/" + conversationId);
        String text = caption == null ? "" : caption.trim();
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }

        Message saved = saveMessage(conversation, user, text, imageUrl, MessageType.TEXT);
        MessageResponse response = toResponse(saved);
        pushAfterCommit(Set.of(conversation.getPoster().getUsername(), conversation.getTrader().getUsername()),
                ChatEvent.message(response));
        return response;
    }

    @Override
    public ConversationResponse updateItemStatus(Long conversationId, Status status, User user) {
        Conversation conversation = getParticipantConversation(conversationId, user);
        if (!conversation.isPoster(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the item owner can change its status");
        }

        Item item = conversation.getItem();
        if (item.getStatus() == status) {
            return toResponse(conversation, user);
        }

        String systemText;
        if (status == Status.TRADED) {
            item.setStatus(Status.TRADED);
            item.setTradedWith(conversation.getTrader());
            item.setTradedAt(LocalDateTime.now());
            systemText = user.getUsername() + " marked this item as Traded with " + conversation.getTrader().getUsername();
        } else {
            item.setStatus(Status.AVAILABLE);
            item.setTradedWith(null);
            item.setTradedAt(null);
            systemText = user.getUsername() + " marked this item as Available again";
        }
        itemRepository.save(item);

        Message systemMessage = saveMessage(conversation, user, systemText, null, MessageType.SYSTEM);

        // Everyone chatting about this item sees the new status; the system message
        // goes only to the two people in this thread.
        Set<String> everyoneOnItem = new LinkedHashSet<>();
        for (Conversation c : conversationRepository.findByItemId(item.getId())) {
            everyoneOnItem.add(c.getPoster().getUsername());
            everyoneOnItem.add(c.getTrader().getUsername());
        }
        pushAfterCommit(everyoneOnItem,
                ChatEvent.status(new ItemStatusEvent(item.getId(), item.getStatus(), item.getTradedWithUserId())));
        pushAfterCommit(Set.of(conversation.getPoster().getUsername(), conversation.getTrader().getUsername()),
                ChatEvent.message(toResponse(systemMessage)));

        return toResponse(conversation, user);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return messageRepository.countUnreadForUser(user.getId());
    }

    // ---------------------------------------------------------------- helpers

    private Conversation getParticipantConversation(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!conversation.isParticipant(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this conversation");
        }
        return conversation;
    }

    private Message saveMessage(Conversation conversation, User sender, String content, String imageUrl,
            MessageType type) {
        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .imageUrl(imageUrl)
                .type(type)
                .build());
        String preview = imageUrl == null ? content
                : (content.isEmpty() ? "📷 Photo" : "📷 " + content);
        conversation.setLastMessageAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());
        conversation.setLastMessagePreview(
                preview.length() > PREVIEW_LENGTH ? preview.substring(0, PREVIEW_LENGTH) + "…" : preview);
        conversationRepository.save(conversation);
        return message;
    }

    /** Send over WebSocket only once the DB transaction has committed. */
    private void pushAfterCommit(Set<String> usernames, ChatEvent event) {
        Runnable send = () -> usernames.forEach(u -> messagingTemplate.convertAndSendToUser(u, USER_QUEUE, event));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private ConversationResponse toResponse(Conversation c, User viewer) {
        Item item = c.getItem();
        User other = c.otherParticipant(viewer);
        return new ConversationResponse(
                c.getId(),
                item.getId(),
                item.getTitle(),
                item.getThumbnailUrl(),
                item.getStatus(),
                item.getTradedWithUserId(),
                other.getId(),
                other.getUsername(),
                c.isPoster(viewer),
                c.getLastMessagePreview(),
                c.getLastMessageAt(),
                messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(c.getId(), viewer.getId()),
                messageRepository.countByConversationIdAndImageUrlIsNotNull(c.getId()),
                maxImagesPerConversation,
                c.getCreatedAt());
    }

    private MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getUsername(),
                m.getContent(),
                m.getImageUrl(),
                m.getType(),
                Boolean.TRUE.equals(m.getIsRead()),
                m.getCreatedAt());
    }
}
