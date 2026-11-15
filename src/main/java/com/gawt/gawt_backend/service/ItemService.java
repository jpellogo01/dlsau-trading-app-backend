package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.MyPostResponse;
import com.gawt.gawt_backend.dto.UpdateItemRequest;
import com.gawt.gawt_backend.model.Condition;
import com.gawt.gawt_backend.model.Item;
import com.gawt.gawt_backend.model.Status;
import com.gawt.gawt_backend.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ItemService {
    Item createItem(String title,
            MultipartFile thumbnail,
            String description,
            String category,
            Condition condition,
            String meetupLocation,
            MultipartFile[] additionalImages,
            User user);

    List<Item> getAllItems();

    /** The logged-in user's own posts, newest first. */
    List<MyPostResponse> getMyPosts(User user);

    /** Owner only: edit title, description, category, condition, meetup location. */
    MyPostResponse updateItem(Long itemId, UpdateItemRequest request, User user);

    /** Owner only: mark AVAILABLE or TRADED from "My Posts". */
    MyPostResponse updateStatus(Long itemId, Status status, User user);

    /** Owner (or admin) only: delete the post with its comments, likes, chats and notifications. */
    void deleteItem(Long itemId, User user);
}
