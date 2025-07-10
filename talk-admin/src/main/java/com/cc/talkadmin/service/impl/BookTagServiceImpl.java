package com.cc.talkadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cc.talkadmin.mapper.BookTagRelationMapper;
import com.cc.talkadmin.mapper.CategoryMapper;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.Result.TagUpResult;
import com.cc.talkpojo.dto.BookEsDTO;
import com.cc.talkpojo.dto.BookTagDTO;
import com.cc.talkpojo.dto.PageTagDTO;
import com.cc.talkpojo.entity.Book;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.entity.BookTag;
import com.cc.talkadmin.mapper.BookTagMapper;
import com.cc.talkadmin.service.IBookTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.talkpojo.entity.BookTagRelation;
import com.cc.talkpojo.vo.BookTagVO;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 图书标签表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-07-09
 */
@Service
public class BookTagServiceImpl extends ServiceImpl<BookTagMapper, BookTag> implements IBookTagService {

    @Resource
    private BookTagMapper bookTagMapper;

    @Resource
    private BookTagRelationMapper bookTagRelationMapper;
    @Autowired
    private CategoryMapper categoryMapper;


    /**
     * 标签分页查询
     * @param pageTagDTO
     * @return
     */
    @Override
    public PageResult<BookTagVO> getPage(PageTagDTO pageTagDTO) {
        //1. 检查分页参数
        CheckPageParam.checkPageDTO(pageTagDTO);

        //2. 开始分页
        PageHelper.startPage(pageTagDTO.getPageNum(), pageTagDTO.getPageSize());

        //3. 分页查询
        LambdaQueryWrapper<BookTag> queryWrapper = new LambdaQueryWrapper<>();
        if(pageTagDTO.getName()!=null&& !pageTagDTO.getName().isEmpty()){
            queryWrapper.like(BookTag::getName, pageTagDTO.getName());
        }
        if(pageTagDTO.getCategoryId()!=null){
            queryWrapper.eq(BookTag::getCategoryId,pageTagDTO.getCategoryId());
        }
        if(pageTagDTO.getCreatedFrom()!=null && pageTagDTO.getCreatedTo()!= null){
            queryWrapper.between(BookTag::getCreateTime, pageTagDTO.getCreatedFrom(), pageTagDTO.getCreatedTo());
        }
        List<BookTag> bookTags = baseMapper.selectList(queryWrapper);

        //4. 转换VO数据
        List<BookTagVO> bookTagVOS = bookTags.stream().map(
                bookTag -> {return ConvertUtils.convert( bookTag, BookTagVO.class );}
        ).collect(Collectors.toList());

        return new PageResult<>(bookTagVOS.size(),bookTagVOS);
    }

    /**
     * 新增单个标签
     * @param bookTagDTO
     */
    @Override
    public void tagSignalAdd(BookTagDTO bookTagDTO) {

        if(bookTagDTO == null || bookTagDTO.getName() == null ||  bookTagDTO.getName().isEmpty()){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 检查标签是否已存在
        if(checkTagExist(bookTagDTO.getName())){
            throw new BaseException(BusinessConstant.TAG_NAME_REPEAT);
        }
        BookTag bookTag = ConvertUtils.convert(bookTagDTO, BookTag.class);
        bookTag.setCreateTime(LocalDateTime.now());
        bookTag.setUpdateTime(LocalDateTime.now());
        baseMapper.insert(bookTag);
    }

    /**
     * 批量新增标签
     * @param bookTagsList
     * @return
     */
    @Override
    public TagUpResult tagsBatchAdd(List<BookTagDTO> bookTagsList) {
        if(bookTagsList == null || bookTagsList.isEmpty()){
            throw new BaseException("参数为空!");
        }
        List<String> existTagList = new ArrayList<>();
        List<BookTag> saveTagList = new ArrayList<>();
        List<String> emptyTagList = new ArrayList<>();

        for(BookTagDTO bookTagDTO : bookTagsList){
            String tagName = bookTagDTO.getName();
            //检查标签名是否为空
            if( tagName == null || tagName.isEmpty()){
                emptyTagList.add("标签描述:"+bookTagDTO.getDescription());
                continue;
            }
            // 检查标签是否已存在
            if(checkTagExist(bookTagDTO.getName())){
                existTagList.add(tagName);
                continue;
            }

            BookTag bookTag = ConvertUtils.convert(bookTagDTO, BookTag.class);
            bookTag.setCreateTime(LocalDateTime.now());
            bookTag.setUpdateTime(LocalDateTime.now());
            saveTagList.add(bookTag);
        }

        boolean flag = saveBatch(saveTagList);
        if(!flag){
            throw new BaseException("批量插入标签失败!");
        }
        return new TagUpResult(saveTagList.size(), emptyTagList, existTagList);
    }

    /**
     * 查询标签详情
     * @param id
     * @return
     */
    @Override
    public BookTagVO getDetail(Long id) {

        if(id==null || id < 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        BookTag bookTag = baseMapper.selectById(id);
        if(bookTag==null){
            throw new BaseException(BusinessConstant.TAG_NAME_NOTEXIST);
        }
        BookTagVO bookTagVO = ConvertUtils.convert(bookTag, BookTagVO.class);
        //获取分类名
        BookCategory bookCategory = categoryMapper.selectById(bookTag.getCategoryId());
        if(bookCategory != null){
            String categoryName = bookCategory.getName();
            bookTagVO.setCategoryName(categoryName);
        }
        return bookTagVO;
    }

    /**
     * 修改标签
     * @param id
     * @param bookTagDTO
     */
    @Override
    public void tagRevise(Long id, BookTagDTO bookTagDTO) {
        if(id==null || id < 0 || !bookTagDTO.getId().equals(id)){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        //查询标签是否存在
        BookTag oldTag = baseMapper.selectById(id);
        if (oldTag == null) {
            throw new BaseException(BusinessConstant.TAG_NAME_NOTEXIST);
        }
        BookTag bookTag = ConvertUtils.convert(bookTagDTO, BookTag.class);
        bookTag.setUpdateTime(LocalDateTime.now());
        bookTag.setId(id);
        int num = baseMapper.updateById(bookTag);
        if(num != 1){
            throw new BaseException(BusinessConstant.TAG_UPDATE_ERROR);
        }
    }

    /**
     * 删除单个标签
     * @param id
     */
    @Override
    public void tagSignalDelete(Long id) {
        if(id==null || id < 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Long count = bookTagRelationMapper.selectCount(
                Wrappers.<BookTagRelation>lambdaQuery().eq(BookTagRelation::getTagId, id)
        );
        if(count>0){
            throw new BaseException(BusinessConstant.TAG_WITH_BOOKS);
        }
        int num = bookTagMapper.deleteById(id);
        if(num != 1){
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR);
        }
    }


    /**
     * 批量删除标签
     * @param idList
     */
    @Override
    public void tagsBatchDelete(List<Long> idList) {
        if(idList == null || idList.isEmpty()){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 查询这些标签中，有哪些是被书籍引用的
        List<Long> usedTagIds = bookTagRelationMapper.selectUsedTagIds(idList);

        if (!usedTagIds.isEmpty()) {
            throw new BaseException(BusinessConstant.TAG_WITH_BOOKS + ":" + usedTagIds);
        }
        int num = bookTagMapper.deleteByIds(idList);
        if(num == 0){
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR + ":" + idList);
        }
    }

    /**
     * 根据分类 ID获取标签
     * @param categoryId
     * @return
     */
    @Override
    public List<BookTagVO> getByCategoryId(Long categoryId) {
        if(categoryId==null || categoryId < 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        LambdaQueryWrapper<BookTag> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(BookTag::getCategoryId, categoryId);

        List<BookTagVO> bookTagVOList = bookTagMapper.selectList(queryWrapper).stream()
                .map(bookTag -> ConvertUtils.convert(bookTag, BookTagVO.class))
                .collect(Collectors.toList());
        if(bookTagVOList.isEmpty()){
            throw new BaseException(BusinessConstant.CATEGROY_WITH_TAGS);
        }

        return bookTagVOList;
    }


    /**
     * 查询标签名是否重复
     * @param name
     * @return
     */
    @Override
    public boolean checkTagExist(String name) {
        if(name==null || name.isEmpty()){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        LambdaQueryWrapper<BookTag> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(BookTag::getName, name);
        Long count = bookTagMapper.selectCount(queryWrapper);
        return count>0;
    }

}
