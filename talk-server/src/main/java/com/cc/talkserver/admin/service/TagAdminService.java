package com.cc.talkserver.admin.service;

import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.Result.TagUpResult;
import com.cc.talkpojo.dto.PageTagDTO;
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
 * @since 2025-07-14
 */
public interface TagAdminService extends IService<Tag> {


    /**
     * 标签分页查询
     * @param pageTagDTO
     * @return
     */
    PageResult<TagVO> getPage(PageTagDTO pageTagDTO);

    /**
     * 新增单个标签
     * @param bookTagDTO
     */
    void tagSignalAdd(TagDTO bookTagDTO);

    /**
     * 批量新增标签
     * @param bookTagsList
     * @return
     */
    TagUpResult tagsBatchAdd(List<TagDTO> bookTagsList);

    /**
     * 查询标签详情
     * @param id
     * @return
     */
    TagVO getDetail(Long id);

    /**
     * 修改标签
     * @param id
     * @param bookTagDTO
     */
    void tagRevise(Long id, TagDTO bookTagDTO);


    /**
     * 删除单个标签
     * @param id
     */
    void tagSignalDelete(Long id);


    /**
     * 批量删除标签
     * @param idList
     */
    void tagsBatchDelete(List<Long> idList);

    /**
     * 根据分类 ID 获取标签
     * @param categoryId
     * @return
     */
    List<TagVO> getByCategoryId(Long categoryId);


    /**
     * 校验标签名称是否重复
     * @param name
     * @return
     */
    boolean checkTagExist(String name);

}
