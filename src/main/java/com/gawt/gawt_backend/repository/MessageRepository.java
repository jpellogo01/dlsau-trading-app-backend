package com.gawt.gawt_backend.repository;

import com.gawt.gawt_backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationIdAndSenderIdNotAndIsReadFalse(Long conversationId, Long userId);

    long countByConversationIdAndImageUrlIsNotNull(Long conversationId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.isRead = false AND m.sender.id <> :userId
              AND (m.conversation.poster.id = :userId OR m.conversation.trader.id = :userId)
            """)
    long countUnreadForUser(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE Message m SET m.isRead = true
            WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.isRead = false
            """)
    int markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.conversation.id IN (SELECT c.id FROM Conversation c WHERE c.item.id = :itemId)")
    int deleteByItemId(@Param("itemId") Long itemId);
}
