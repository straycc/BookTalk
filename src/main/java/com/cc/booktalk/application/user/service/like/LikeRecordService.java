package com.cc.booktalk.application.user.service.like;

import com.cc.booktalk.entity.result.PageResult;
import com.cc.booktalk.entity.dto.like.LikePageDTO;
import com.cc.booktalk.entity.dto.like.LikeRecordDTO;
import com.cc.booktalk.entity.entity.like.LikeRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.booktalk.entity.vo.LikeRecordVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author cc
 * @since 2025-09-10
 */
public interface LikeRecordService extends IService<LikeRecord> {


    /**
     * 用户点赞
     * @param likeRecordDTO
     */
    void clickLike(LikeRecordDTO likeRecordDTO);


    /**
     * 查询点赞状态
     * @param likeRecordDTO
     */
    boolean getLikeStatus(LikeRecordDTO likeRecordDTO);


    /**
     * 查询目标点赞数量
     * @param targetId
     * @param likeRecordDTO
     * @return
     */
    Long getLikeCount(Long targetId, LikeRecordDTO likeRecordDTO);


    /**
     * 查询用户点赞动态
     * @param likePageDTO
     * @return
     */
    PageResult<LikeRecordVO> likeDynamicPage(LikePageDTO likePageDTO);
}
