package com.gawt.gawt_backend.controller;

import com.gawt.gawt_backend.dto.ItemRequest;
import com.gawt.gawt_backend.model.Condition;
import com.gawt.gawt_backend.model.Item;
import com.gawt.gawt_backend.model.TradeOption;
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
            @RequestParam(required = false) Double price,
            @RequestParam(defaultValue = "false") boolean isNegotiable,
            @RequestParam TradeOption tradeOption,
            @RequestParam Condition condition,
            @RequestParam(required = false) String meetupLocation,
            @RequestPart(required = false) MultipartFile thumbnail,
            @RequestPart(required = false) MultipartFile[] additionalImages) {
        Item savedItem = itemService.createItem(
                title, thumbnail, description, category, price, isNegotiable, tradeOption, condition, meetupLocation,
                additionalImages, user);
        return ResponseEntity.ok(savedItem);
    }

    @GetMapping("/items")
    public ResponseEntity<List<Item>> getAllItems() {
        List<Item> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }
}