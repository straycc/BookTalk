package com.cc.talkserver.user.service;

import com.cc.talkpojo.dto.TagDTO;
import com.cc.talkpojo.entity.Tag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.vo.TagVO;

import java.util.List;

/**
 * <p>
 * 标签表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-08-02
 */
public interface TagUserService extends IService<Tag> {

    /**
     * 根据分类id查询标签列表
     * @param categoryId
     * @return
     */
    List<TagVO> findByCategoryId(Long categoryId);

    /**
     * 查询标签详情
     * @param tagId
     * @return
     */
    TagVO getTagDetail(Long tagId);

    /**
     * 建立新标签
     * @param tagDTO
     */
    void creatNewTag(TagDTO tagDTO);


    /**
     * 删除标签
     * @param tagId
     */
    void deleteTagById(Long tagId);


    /**
     * 用户修改标签
     * @param tagDTO
     */
    void updateTag(TagDTO tagDTO);

    /**
     * 获取用户创建的所有标签
     * @param userId
     * @return
     */
    List<TagVO> getUserTags(Long userId);


    /**
     * 根据标签名查询标签
     * @param tagName
     * @return
     */
    TagVO getByTagName(String tagName);
}
