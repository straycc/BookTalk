package com.cc.booktalk.application.user.service.notification;

import com.cc.booktalk.common.websocket.WebSocketMessage;

public interface WebSocketPushPort {

    void pushToUser(Long userId, WebSocketMessage<?> message);
}
