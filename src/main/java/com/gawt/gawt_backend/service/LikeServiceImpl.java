package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.LikeResponse;
import com.gawt.gawt_backend.model.Item;
import com.gawt.gawt_backend.model.Like;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.repository.ItemRepository;
import com.gawt.gawt_backend.repository.LikeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final ItemRepository itemRepository;

    @Override
    public LikeResponse addLike(Long itemId, User user) {
        Item item = getItemOrThrow(itemId);

        boolean alreadyLiked = likeRepository.existsByItemIdAndUserId(itemId, user.getId());
        if (!alreadyLiked) {
            Like like = Like.builder()
                    .item(item)
                    .user(user)
                    .build();
            likeRepository.save(like);
        }

        return buildLikeResponse(itemId, user.getId());
    }

    @Override
    public LikeResponse removeLike(Long itemId, User user) {
        getItemOrThrow(itemId);

        likeRepository.findByItemIdAndUserId(itemId, user.getId())
                .ifPresent(likeRepository::delete);

        return buildLikeResponse(itemId, user.getId());
    }

    @Override
    public LikeResponse getLikeSummary(Long itemId, User user) {
        getItemOrThrow(itemId);
        return buildLikeResponse(itemId, user.getId());
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    private LikeResponse buildLikeResponse(Long itemId, Long userId) {
        long totalLikes = likeRepository.countByItemId(itemId);
        boolean likedByCurrentUser = likeRepository.existsByItemIdAndUserId(itemId, userId);
        return new LikeResponse(itemId, totalLikes, likedByCurrentUser);
    }
}
