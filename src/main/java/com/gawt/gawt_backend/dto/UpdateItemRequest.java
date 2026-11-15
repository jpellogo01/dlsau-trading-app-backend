package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.Condition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Editable text fields of a post (photos are not changed here). */
public record UpdateItemRequest(
        @NotBlank(message = "Title is required") @Size(max = 120) String title,
        @Size(max = 5000) String description,
        @Size(max = 60) String category,
        @NotNull(message = "Condition is required") Condition condition,
        @Size(max = 120) String meetupLocation) {
}
