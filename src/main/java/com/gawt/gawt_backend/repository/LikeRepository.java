package com.gawt.gawt_backend.repository;

import com.gawt.gawt_backend.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    long countByItemId(Long itemId);

    boolean existsByItemIdAndUserId(Long itemId, Long userId);

    Optional<Like> findByItemIdAndUserId(Long itemId, Long userId);
}
