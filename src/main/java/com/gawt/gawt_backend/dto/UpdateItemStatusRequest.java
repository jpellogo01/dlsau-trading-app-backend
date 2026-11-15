package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateItemStatusRequest(@NotNull Status status) {
}
