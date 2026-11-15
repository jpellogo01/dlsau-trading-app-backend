package com.gawt.gawt_backend.repository;

import com.gawt.gawt_backend.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /** Posts of one user, newest first ("My Posts"). */
    List<Item> findByUserIdOrderByCreatedAtDesc(Long userId);
}
