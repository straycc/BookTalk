package com.cc.booktalk.application.user.service.comment;

import com.cc.booktalk.entity.dto.comment.CommentDTO;
import com.cc.booktalk.entity.result.PageResult;
import com.cc.booktalk.entity.dto.comment.CommentPageDTO;
import com.cc.booktalk.entity.entity.comment.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.booktalk.entity.vo.CommentVO;

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
     * @param commentDTO
     */
    void commentPublish(Long targetId, CommentDTO commentDTO);


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
