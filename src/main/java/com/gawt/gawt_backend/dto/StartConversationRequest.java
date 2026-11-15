package com.gawt.gawt_backend.dto;

import jakarta.validation.constraints.NotNull;

public record StartConversationRequest(@NotNull Long itemId) {
}
