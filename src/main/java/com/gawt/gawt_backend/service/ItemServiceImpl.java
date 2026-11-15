package com.gawt.gawt_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gawt.gawt_backend.dto.ChatEvent;
import com.gawt.gawt_backend.dto.ItemRequest;
import com.gawt.gawt_backend.dto.ItemStatusEvent;
import com.gawt.gawt_backend.dto.MyPostResponse;
import com.gawt.gawt_backend.dto.UpdateItemRequest;
import com.gawt.gawt_backend.model.*;
import com.gawt.gawt_backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ItemServiceImpl implements ItemService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final Cloudinary cloudinary;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ------------------------------------------------------------ My Posts

    @Override
    public List<MyPostResponse> getMyPosts(User user) {
        return itemRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toMyPost)
                .toList();
    }

    @Override
    public MyPostResponse updateItem(Long itemId, UpdateItemRequest request, User user) {
        Item item = getOwnedItem(itemId, user, false);
        item.setTitle(request.title().trim());
        item.setDescription(blankToNull(request.description()));
        item.setCategory(blankToNull(request.category()));
        item.setCondition(request.condition());
        item.setMeetupLocation(blankToNull(request.meetupLocation()));
        return toMyPost(itemRepository.save(item));
    }

    @Override
    public MyPostResponse updateStatus(Long itemId, Status status, User user) {
        Item item = getOwnedItem(itemId, user, false);
        if (item.getStatus() != status) {
            item.setStatus(status);
            if (status == Status.AVAILABLE) {
                item.setTradedWith(null);
                item.setTradedAt(null);
            } else {
                // Marked from My Posts (not from a chat): trade partner unknown.
                item.setTradedAt(LocalDateTime.now());
            }
            itemRepository.save(item);
            broadcastStatus(item);
        }
        return toMyPost(item);
    }

    @Override
    public void deleteItem(Long itemId, User user) {
        Item item = getOwnedItem(itemId, user, true);
        // Remove everything that points at the item, children before parents.
        notificationRepository.deleteByItemId(itemId);
        messageRepository.deleteByItemId(itemId);
        conversationRepository.deleteByItemId(itemId);
        commentRepository.deleteRepliesByItemId(itemId);
        commentRepository.deleteByItemId(itemId);
        likeRepository.deleteByItemId(itemId);
        itemRepository.delete(item); // item images are removed by cascade
    }

    private Item getOwnedItem(Long itemId, User user, boolean allowAdmin) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        boolean isOwner = item.getUser() != null && item.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOwner && !(allowAdmin && isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only change your own posts");
        }
        return item;
    }

    /** Tell everyone chatting about this item that its status changed (keeps chats/marketplace live). */
    private void broadcastStatus(Item item) {
        ChatEvent event = ChatEvent.status(
                new ItemStatusEvent(item.getId(), item.getStatus(), item.getTradedWithUserId()));
        Set<String> usernames = new LinkedHashSet<>();
        for (Conversation c : conversationRepository.findByItemId(item.getId())) {
            usernames.add(c.getPoster().getUsername());
            usernames.add(c.getTrader().getUsername());
        }
        Runnable send = () -> usernames.forEach(u -> messagingTemplate.convertAndSendToUser(u, "/queue/chat", event));
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

    private MyPostResponse toMyPost(Item item) {
        User tradedWith = item.getTradedWith();
        return new MyPostResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getCategory(),
                item.getCondition(),
                item.getMeetupLocation(),
                item.getThumbnailUrl(),
                item.getStatus(),
                tradedWith != null ? tradedWith.getId() : null,
                tradedWith != null ? tradedWith.getUsername() : null,
                item.getTradedAt(),
                item.getCreatedAt(),
                commentRepository.countByItemId(item.getId()),
                likeRepository.countByItemId(item.getId()),
                conversationRepository.countByItemId(item.getId()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Override
    public Item createItem(String title,
            MultipartFile thumbnail,
            String description,
            String category,
            Condition condition,
            String meetupLocation,
            MultipartFile[] additionalImages,
            User user) {

        try {
            Item item = new Item();
            item.setTitle(title);
            item.setDescription(description);
            item.setCategory(category);
            item.setCondition(condition);
            item.setStatus(Status.AVAILABLE);
            item.setMeetupLocation(meetupLocation);
            item.setUser(user);

            // Upload thumbnail
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String type = thumbnail.getContentType();
                if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
                    throw new RuntimeException("Thumbnail must be JPEG or PNG");
                }
                if (thumbnail.getSize() > 5 * 1024 * 1024) {
                    throw new RuntimeException("Thumbnail exceeds 5MB");
                }

                Map uploadResult = cloudinary.uploader().upload(thumbnail.getBytes(), ObjectUtils.emptyMap());
                item.setThumbnailUrl((String) uploadResult.get("secure_url"));
            }

            // Upload additional images
            if (additionalImages != null) {
                item = itemRepository.save(item); // Save item first to get ID
                List<ItemImage> itemImages = new ArrayList<>();
                for (MultipartFile file : additionalImages) {
                    if (file != null && !file.isEmpty()) {
                        if (file.getSize() > 5 * 1024 * 1024)
                            continue;
                        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
                        String imageUrl = (String) uploadResult.get("secure_url");
                        ItemImage itemImage = ItemImage.builder()
                                .item(item)
                                .imageUrl(imageUrl)
                                .build();
                        itemImages.add(itemImage);
                    }
                }
                itemImageRepository.saveAll(itemImages);
            } else {
                item = itemRepository.save(item);
            }

            return item;

        } catch (IOException e) {
            throw new RuntimeException("Error uploading files to Cloudinary", e);
        }
    }

    @Override
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    private Item buildItem(ItemRequest request, User user, String thumbnailUrl) {
        Item item = new Item();
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setCategory(request.category());
        item.setCondition(request.condition());
        item.setMeetupLocation(request.meetupLocation());
        item.setThumbnailUrl(thumbnailUrl);
        item.setStatus(Status.AVAILABLE);
        item.setUser(user);
        return item;
    }

    private List<ItemImage> uploadAdditionalImages(MultipartFile[] additionalImages, Item item) throws IOException {
        List<ItemImage> images = new ArrayList<>();

        if (additionalImages == null) {
            return images;
        }

        for (MultipartFile file : additionalImages) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            if (!isValidImage(file)) {
                continue;
            }

            String imageUrl = uploadImage(file);
            ItemImage itemImage = new ItemImage();
            itemImage.setItem(item);
            itemImage.setImageUrl(imageUrl);
            images.add(itemImage);
        }

        if (!images.isEmpty()) {
            return itemImageRepository.saveAll(images);
        }

        return images;
    }

    private String uploadImage(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return (String) uploadResult.get("secure_url");
    }

    private void validateImageFile(MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("Thumbnail is required");
            }
            return;
        }

        if (!isValidImage(file)) {
            throw new IllegalArgumentException("Only JPEG and PNG files under 5MB are allowed");
        }
    }

    private boolean isValidImage(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return false;
        }

        String contentType = file.getContentType();
        return "image/jpeg".equals(contentType) || "image/png".equals(contentType);
    }
}
