package com.cc.talkcommon.event;

import com.cc.talkcommon.constant.NotificationConstant;
import com.cc.talkcommon.event.request.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 通知事件发布器
 *
 * @author cc
 * @since 2025-10-12
 */
@Slf4j
@Component
public class NotificationEventPublisher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 发布点赞通知
     * @param request 点赞通知请求
     */
    public void publishLikeEvent(NotificationRequest request) {
        String content = buildLikeContent(request.getTargetType());

        NotificationEvent event = NotificationEvent.builder()
                .userId(request.getUserId())
                .type(NotificationConstant.NOTIFICATION_TYPE_LIKE)
                .title("收到新的点赞")
                .content(content)
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .senderId(request.getSender().getId())
                .senderName(request.getSender().getName())
                .senderAvatar(request.getSender().getAvatar())
                .build();

        publishEvent(event);
        log.info("发布点赞通知: 接收者={}, 发送者={}, 目标类型={}",
                request.getUserId(), request.getSender().getId(), request.getTargetType());
    }

    /**
     * 发布评论通知
     * @param request 评论通知请求
     */
    public void publishCommentEvent(NotificationRequest request) {
        String content = buildCommentContent(request.getTargetType(), request.getContent());

        NotificationEvent event = NotificationEvent.builder()
                .userId(request.getUserId())
                .type(NotificationConstant.NOTIFICATION_TYPE_COMMENT)
                .title("收到新的评论")
                .content(content)
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .senderId(request.getSender().getId())
                .senderName(request.getSender().getName())
                .senderAvatar(request.getSender().getAvatar())
                .build();

        publishEvent(event);
        log.info("发布评论通知: 接收者={}, 发送者={}, 目标类型={}",
                request.getUserId(), request.getSender().getId(), request.getTargetType());
    }

    /**
     * 发布回复通知
     * @param request 回复通知请求
     */
    public void publishReplyEvent(NotificationRequest request) {
        String content = NotificationConstant.TEMPLATE_REPLY_COMMENT.replace("{content}",
                request.getContent().length() > 20 ? request.getContent().substring(0, 20) + "..." : request.getContent());

        NotificationEvent event = NotificationEvent.builder()
                .userId(request.getUserId())
                .type(NotificationConstant.NOTIFICATION_TYPE_REPLY)
                .title("收到新的回复")
                .content(content)
                .targetId(request.getTargetId())
                .targetType(NotificationConstant.TARGET_TYPE_COMMENT)
                .senderId(request.getSender().getId())
                .senderName(request.getSender().getName())
                .senderAvatar(request.getSender().getAvatar())
                .build();

        publishEvent(event);
        log.info("发布回复通知: 接收者={}, 发送者={}",
                request.getUserId(), request.getSender().getId());
    }

    /**
     * 发布关注通知
     * @param request 关注通知请求
     */
    public void publishFollowEvent(NotificationRequest request) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(request.getUserId())
                .type(NotificationConstant.NOTIFICATION_TYPE_FOLLOW)
                .title("收到新的关注")
                .content(NotificationConstant.TEMPLATE_FOLLOW_USER)
                .targetId(request.getTargetId())
                .targetType(NotificationConstant.TARGET_TYPE_USER)
                .senderId(request.getSender().getId())
                .senderName(request.getSender().getName())
                .senderAvatar(request.getSender().getAvatar())
                .build();

        publishEvent(event);
        log.info("发布关注通知: 接收者={}, 发送者={}",
                request.getUserId(), request.getSender().getId());
    }

    /**
     * 发布系统通知
     * @param request 系统通知请求
     */
    public void publishSystemEvent(NotificationRequest request) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(request.getUserId())
                .type(NotificationConstant.NOTIFICATION_TYPE_SYSTEM)
                .title(request.getTitle())
                .content(request.getContent())
                .targetType(NotificationConstant.TARGET_TYPE_USER)
                .build();

        publishEvent(event);
        log.info("发布系统通知: 标题={}, 内容={}", request.getTitle(), request.getContent());
    }

    /**
     * 发布事件 (纯Spring Event)
     * @param event 通知事件
     */
    private void publishEvent(NotificationEvent event) {
        try {
            eventPublisher.publishEvent(event);
            log.info("通知事件发布成功: 类型={}, 接收者={}", event.getType(), event.getUserId());
        } catch (Exception e) {
            log.error("发布通知事件失败", e);
        }
    }

    /**
     * 构建点赞内容
     * @param targetType 目标类型
     * @return 内容文本
     */
    private String buildLikeContent(String targetType) {
        if (NotificationConstant.TARGET_TYPE_BOOK_REVIEW.equals(targetType)) {
            return NotificationConstant.TEMPLATE_LIKE_REVIEW;
        } else if (NotificationConstant.TARGET_TYPE_COMMENT.equals(targetType)) {
            return NotificationConstant.TEMPLATE_LIKE_COMMENT;
        }
        return "赞了你的内容";
    }

    /**
     * 构建评论内容
     * @param targetType 目标类型
     * @param commentContent 评论内容
     * @return 内容文本
     */
    private String buildCommentContent(String targetType, String commentContent) {
        if (NotificationConstant.TARGET_TYPE_BOOK_REVIEW.equals(targetType)) {
            String truncatedContent = commentContent.length() > 30 ?
                    commentContent.substring(0, 30) + "..." : commentContent;
            return NotificationConstant.TEMPLATE_COMMENT_REVIEW.replace("{content}", truncatedContent);
        }
        return "评论了你的内容：" + commentContent;
    }
}