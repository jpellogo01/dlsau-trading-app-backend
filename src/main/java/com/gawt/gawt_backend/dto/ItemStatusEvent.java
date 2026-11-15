package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.Status;

public record ItemStatusEvent(Long itemId, Status status, Long tradedWithUserId) {
}
