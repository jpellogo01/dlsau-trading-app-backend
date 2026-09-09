package com.gawt.gawt_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gawt.gawt_backend.dto.ItemRequest;
import com.gawt.gawt_backend.model.*;
import com.gawt.gawt_backend.repository.ItemImageRepository;
import com.gawt.gawt_backend.repository.ItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ItemServiceImpl implements ItemService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final Cloudinary cloudinary;

    @Override
    public Item createItem(String title,
            MultipartFile thumbnail,
            String description,
            String category,
            Double price,
            boolean isNegotiable,
            TradeOption tradeOption,
            Condition condition,
            String meetupLocation,
            MultipartFile[] additionalImages,
            User user) {

        try {
            Item item = new Item();
            item.setTitle(title);
            item.setDescription(description);
            item.setCategory(category);
            item.setPrice(price);
            item.setIsNegotiable(isNegotiable);
            item.setTradeOption(tradeOption);
            item.setCondition(condition);
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
        item.setPrice(request.price());
        item.setIsNegotiable(request.isNegotiable());
        item.setTradeOption(request.tradeOption());
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
