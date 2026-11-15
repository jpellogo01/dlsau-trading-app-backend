package com.gawt.gawt_backend.controller;

import com.gawt.gawt_backend.dto.ItemRequest;
import com.gawt.gawt_backend.dto.MyPostResponse;
import com.gawt.gawt_backend.dto.UpdateItemRequest;
import com.gawt.gawt_backend.dto.UpdateItemStatusRequest;
import com.gawt.gawt_backend.model.Condition;
import com.gawt.gawt_backend.model.Item;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Item> createItem(
            @AuthenticationPrincipal User user,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam Condition condition,
            @RequestParam(required = false) String meetupLocation,
            @RequestPart(required = false) MultipartFile thumbnail,
            @RequestPart(required = false) MultipartFile[] additionalImages) {
        Item savedItem = itemService.createItem(
                title, thumbnail, description, category, condition, meetupLocation,
                additionalImages, user);
        return ResponseEntity.ok(savedItem);
    }

    @GetMapping("/items")
    public ResponseEntity<List<Item>> getAllItems() {
        List<Item> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    // ------------------------------------------------------------ My Posts (owner only)

    @GetMapping("/items/mine")
    public ResponseEntity<List<MyPostResponse>> getMyPosts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemService.getMyPosts(user));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<MyPostResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemService.updateItem(itemId, request, user));
    }

    @PatchMapping("/items/{itemId}/status")
    public ResponseEntity<MyPostResponse> updateStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemStatusRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemService.updateStatus(itemId, request.status(), user));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId, @AuthenticationPrincipal User user) {
        itemService.deleteItem(itemId, user);
        return ResponseEntity.noContent().build();
    }
}