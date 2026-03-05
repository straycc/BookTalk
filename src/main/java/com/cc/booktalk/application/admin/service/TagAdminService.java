package com.cc.booktalk.application.admin.service;

import com.cc.booktalk.common.result.PageResult;
import com.cc.booktalk.common.result.TagUpResult;
import com.cc.booktalk.interfaces.dto.user.tag.PageTagDTO;
import com.cc.booktalk.interfaces.dto.user.tag.TagDTO;
import com.cc.booktalk.domain.entity.tag.Tag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.booktalk.interfaces.vo.user.tag.TagVO;

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
