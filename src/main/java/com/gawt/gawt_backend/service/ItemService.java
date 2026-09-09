package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.model.Condition;
import com.gawt.gawt_backend.model.Item;
import com.gawt.gawt_backend.model.TradeOption;
import com.gawt.gawt_backend.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ItemService {
    Item createItem(String title,
            MultipartFile thumbnail,
            String description,
            String category,
            Double price,
            boolean isNegotiable,
            TradeOption tradeOption,
            Condition condition,
            String meetupLocation,
            MultipartFile[] additionalImages,
            User user);

    List<Item> getAllItems();
}
