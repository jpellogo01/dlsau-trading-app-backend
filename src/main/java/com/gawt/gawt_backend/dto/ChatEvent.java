package com.gawt.gawt_backend.dto;

/**
 * Envelope pushed to users over WebSocket at /user/queue/chat.
 * type = MESSAGE -> payload is MessageResponse
 * type = STATUS  -> payload is ItemStatusEvent
 */
public record ChatEvent(String type, Object payload) {
    public static ChatEvent message(MessageResponse m) {
        return new ChatEvent("MESSAGE", m);
    }

    public static ChatEvent status(ItemStatusEvent s) {
        return new ChatEvent("STATUS", s);
    }
}
