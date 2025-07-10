package com.cc.talkadmin.service;

import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.Result.TagUpResult;
import com.cc.talkpojo.dto.BookTagDTO;
import com.cc.talkpojo.dto.PageTagDTO;
import com.cc.talkpojo.entity.BookTag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.talkpojo.vo.BookTagVO;

import java.util.List;

/**
 * <p>
 * 图书标签表 服务类
 * </p>
 *
 * @author cc
 * @since 2025-07-09
 */
public interface IBookTagService extends IService<BookTag> {

    /**
     * 标签分页查询
     * @param pageTagDTO
     * @return
     */
    PageResult<BookTagVO> getPage(PageTagDTO pageTagDTO);

    /**
     * 新增单个标签
     * @param bookTagDTO
     */
    void tagSignalAdd(BookTagDTO bookTagDTO);

    /**
     * 批量新增标签
     * @param bookTagsList
     * @return
     */
    TagUpResult tagsBatchAdd(List<BookTagDTO> bookTagsList);

    /**
     * 查询标签详情
     * @param id
     * @return
     */
    BookTagVO getDetail(Long id);

    /**
     * 修改标签
     * @param id
     * @param bookTagDTO
     */
    void tagRevise(Long id, BookTagDTO bookTagDTO);


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
    List<BookTagVO> getByCategoryId(Long categoryId);


    /**
     * 校验标签名称是否重复
     * @param name
     * @return
     */
    boolean checkTagExist(String name);
}
