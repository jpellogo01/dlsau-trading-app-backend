package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.ConversationResponse;
import com.gawt.gawt_backend.dto.MessageResponse;
import com.gawt.gawt_backend.model.Status;
import com.gawt.gawt_backend.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {

    ConversationResponse startOrGetConversation(Long itemId, User user);

    List<ConversationResponse> getMyConversations(User user);

    ConversationResponse getConversation(Long conversationId, User user);

    List<MessageResponse> getMessages(Long conversationId, User user);

    MessageResponse sendMessage(Long conversationId, String content, User user);

    /** One photo per message, optional caption; limited per conversation. */
    MessageResponse sendImageMessage(Long conversationId, MultipartFile image, String caption, User user);

    ConversationResponse updateItemStatus(Long conversationId, Status status, User user);

    long getUnreadCount(User user);
}
