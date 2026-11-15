package com.gawt.gawt_backend.repository;

import com.gawt.gawt_backend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByItemIdAndTraderId(Long itemId, Long traderId);

    List<Conversation> findByItemId(Long itemId);

    long countByItemId(Long itemId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.poster.id = :userId OR c.trader.id = :userId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Conversation c WHERE c.item.id = :itemId")
    int deleteByItemId(@Param("itemId") Long itemId);
}
