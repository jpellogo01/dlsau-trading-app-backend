package com.gawt.gawt_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(name = "item_condition", nullable = false)
    @NotNull(message = "Condition is required")
    @Enumerated(EnumType.STRING)
    private Condition condition;

    private String meetupLocation;

    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.AVAILABLE;

    /** The user this item was traded with (set by the poster from a chat). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traded_with_user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User tradedWith;

    private LocalDateTime tradedAt;

    /** Exposes only the id of the trade partner in JSON responses. */
    public Long getTradedWithUserId() {
        return tradedWith != null ? tradedWith.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImage> images = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
