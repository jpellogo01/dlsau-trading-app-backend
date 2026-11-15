package com.gawt.gawt_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A private chat thread between the poster of an item and one interested trader.
 * Exactly one thread exists per (item, trader) pair.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = { "item_id", "trader_id" }))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** The user who posted the item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id", nullable = false)
    private User poster;

    /** The user who is interested in trading for the item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trader_id", nullable = false)
    private User trader;

    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public boolean isParticipant(User user) {
        return user != null && (poster.getId().equals(user.getId()) || trader.getId().equals(user.getId()));
    }

    public boolean isPoster(User user) {
        return user != null && poster.getId().equals(user.getId());
    }

    public User otherParticipant(User user) {
        return isPoster(user) ? trader : poster;
    }
}
