package com.gawt.gawt_backend.repository;

import com.gawt.gawt_backend.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByItemIdOrderByCreatedAtDesc(Long itemId);

    List<Comment> findByParentId(Long parentId);

    long countByItemId(Long itemId);

    /** Replies first (they point at their parent), then top-level comments. */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.item.id = :itemId AND c.parent IS NOT NULL")
    int deleteRepliesByItemId(@Param("itemId") Long itemId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.item.id = :itemId")
    int deleteByItemId(@Param("itemId") Long itemId);
}
