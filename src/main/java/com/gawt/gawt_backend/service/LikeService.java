package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.LikeResponse;
import com.gawt.gawt_backend.model.User;

public interface LikeService {
    LikeResponse addLike(Long itemId, User user);

    LikeResponse removeLike(Long itemId, User user);

    LikeResponse getLikeSummary(Long itemId, User user);
}
