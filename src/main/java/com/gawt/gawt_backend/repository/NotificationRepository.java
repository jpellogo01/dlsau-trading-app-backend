package com.gawt.gawt_backend.repository;

import com.gawt.gawt_backend.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllRead(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.comment.id IN :commentIds")
    int deleteByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.item.id = :itemId")
    int deleteByItemId(@Param("itemId") Long itemId);
}
