package com.brushing.web.controller.websocket;

import com.alibaba.fastjson2.JSONObject;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@ServerEndpoint("/websocket/message/{userId}")
public class WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private static final Map<Long, Session> sessions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private static final long HEARTBEAT_INTERVAL = 30000;  // 30秒
    private static final int MAX_CONNECTIONS = 1000;  // 最大连接数

    // 手动注入 IOrderMemberUserService
    private static IOrderMemberUserService orderMemberUserService;

    // 使用静态方法来初始化 Spring 上下文并获取依赖
    @Autowired
    public void setOrderMemberUserService(IOrderMemberUserService orderMemberUserService) {
        WebSocketServer.orderMemberUserService = orderMemberUserService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) throws IOException {
        if (sessions.size() >= MAX_CONNECTIONS) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "99");
            jsonObject.put("data", "Server is busy, too many connections.");
            session.getBasicRemote().sendText(jsonObject.toString());
            session.close();
            return;
        }

        OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(userId);
        if (StringUtils.isNull(orderMemberUser)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "99");
            jsonObject.put("data", "user not found, connection is closed.");
            session.getBasicRemote().sendText(jsonObject.toString());
            session.close();
            return;
        }

        // 关闭旧连接
        if (sessions.containsKey(userId)) {
            Session oldSession = sessions.get(userId);
            try {
                oldSession.close();
                logger.info("Closed existing session for user {}", userId);
            } catch (IOException e) {
                logger.error("Failed to close old session for user {}", userId, e);
            }
        }

        sessions.put(userId, session);
        session.setMaxIdleTimeout(300000);  // 5分钟空闲超时
        logger.info("User {} connected.", userId);

        startHeartbeat(session, userId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") Long userId) {
        logger.info("Received message from user {}: {}", userId, message);
        // 处理消息逻辑
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        sessions.remove(userId);
        cancelHeartbeat(userId);
        logger.info("User {} disconnected.", userId);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        logger.error("Error for user {}: {}", userId, error.getMessage(), error);
        cancelHeartbeat(userId);
        try {
            session.close();
        } catch (IOException e) {
            logger.error("Failed to close session for user {}", userId, e);
        }
    }

    public static void sendMessageToUser(Long userId, String message) {
        Session session = sessions.get(userId);
        if (session != null) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.error("Failed to send message to user {}", userId, e);
                    sessions.remove(userId);
                }
            } else {
                sessions.remove(userId);
            }
        }
    }

    private void startHeartbeat(Session session, Long userId) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendPing(ByteBuffer.allocate(0));
                    logger.info("Sent heartbeat to user {}", userId);
                } catch (IOException e) {
                    logger.error("Heartbeat failed for user {}", userId, e);
                }
            } else {
                cancelHeartbeat(userId);
            }
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        heartbeatTasks.put(userId, task);
    }

    private void cancelHeartbeat(Long userId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(userId);
        if (task != null) {
            task.cancel(true);
            logger.info("Cancelled heartbeat for user {}", userId);
        }
    }
}