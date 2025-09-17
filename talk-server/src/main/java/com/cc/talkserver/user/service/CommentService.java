package com.cc.talkserver.user.service;

import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.CommentPageDTO;
import com.cc.talkpojo.entity.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.vo.CommentVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author cc
 * @since 2025-09-08
 */
public interface CommentService extends IService<Comment> {


    /**
     * 发布评论
     * @param targetId
     * @param commentVO
     */
    void commentPublish(Long targetId, CommentVO commentVO);


    /**
     * 删除评论成功
     * @param commentId
     */
    void deleteComment(Long commentId);

    /**
     * 查询用户的所有评论
     * @param userId
     * @param commentPageDTO
     * @return
     */
    PageResult<CommentVO> getUserAllComments(Long userId, CommentPageDTO commentPageDTO);


    /**
     * 查询某个书评所有评论
     * @param bookReviewId
     * @return
     */
    List<CommentVO> bookReviewAllCommments(Long bookReviewId);
}
