package com.cc.talkserver.user.service;

import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.dto.LikePageDTO;
import com.cc.talkpojo.dto.LikeRecordDTO;
import com.cc.talkpojo.entity.LikeRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.vo.LikeRecordVO;

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
