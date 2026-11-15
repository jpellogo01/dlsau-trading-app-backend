package com.gawt.gawt_backend.repository;

import com.gawt.gawt_backend.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    long countByItemId(Long itemId);

    boolean existsByItemIdAndUserId(Long itemId, Long userId);

    Optional<Like> findByItemIdAndUserId(Long itemId, Long userId);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.item.id = :itemId")
    int deleteByItemId(@Param("itemId") Long itemId);
}
