package com.cc.talkserver.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.Result.PageResult;
import com.cc.talkpojo.Result.TagUpResult;
import com.cc.talkpojo.dto.BookTagDTO;
import com.cc.talkpojo.dto.PageTagDTO;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.entity.BookTagRelation;
import com.cc.talkpojo.entity.Tag;
import com.cc.talkpojo.vo.TagVO;
import com.cc.talkserver.admin.mapper.BookTagAdminMapper;
import com.cc.talkserver.admin.mapper.CategoryAdminMapper;
import com.cc.talkserver.admin.mapper.TagAdminMapper;
import com.cc.talkserver.admin.service.TagAdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 标签表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-07-14
 */
@Service
public class TagAdminServiceImpl extends ServiceImpl<TagAdminMapper, Tag> implements TagAdminService {



    @Resource
    private CategoryAdminMapper categoryAdminMapper;

    @Resource
    private TagAdminMapper tagAdminMapper;

    @Resource
    private BookTagAdminMapper bookTagAdminMapper;


    /**
     * 标签分页查询
     * @param pageTagDTO
     * @return
     */
    @Override
    public PageResult<TagVO> getPage(PageTagDTO pageTagDTO) {
        //1. 检查分页参数
        CheckPageParam.checkPageDTO(pageTagDTO);

        //2. 开始分页
        PageHelper.startPage(pageTagDTO.getPageNum(), pageTagDTO.getPageSize());

        //3. 分页查询
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        if(pageTagDTO.getName()!=null&& !pageTagDTO.getName().isEmpty()){
            queryWrapper.like(Tag::getName, pageTagDTO.getName());
        }
        if(pageTagDTO.getCategoryId()!=null){
            queryWrapper.eq(Tag::getCategoryId,pageTagDTO.getCategoryId());
        }
        if(pageTagDTO.getCreatedFrom()!=null && pageTagDTO.getCreatedTo()!= null){
            queryWrapper.between(Tag::getCreateTime, pageTagDTO.getCreatedFrom(), pageTagDTO.getCreatedTo());
        }
        List<Tag> tags = tagAdminMapper.selectList(queryWrapper);

        //4. 转换VO数据
        List<TagVO> tagVOS = tags.stream().map(
                bookTag -> {return ConvertUtils.convert( bookTag, TagVO.class );}
        ).collect(Collectors.toList());

        return new PageResult<>(tagVOS.size(), tagVOS);
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
        Tag tag = ConvertUtils.convert(bookTagDTO, Tag.class);
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        tagAdminMapper.insert(tag);
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
        List<Tag> saveTagList = new ArrayList<>();
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

            Tag tag = ConvertUtils.convert(bookTagDTO, Tag.class);
            tag.setCreateTime(LocalDateTime.now());
            tag.setUpdateTime(LocalDateTime.now());
            saveTagList.add(tag);
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
    public TagVO getDetail(Long id) {

        if(id==null || id < 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Tag tag = tagAdminMapper.selectById(id);
        if(tag ==null){
            throw new BaseException(BusinessConstant.TAG_NAME_NOTEXIST);
        }
        TagVO tagVO = ConvertUtils.convert(tag, TagVO.class);
        //获取分类名
        BookCategory bookCategory = categoryAdminMapper.selectById(tag.getCategoryId());
        if(bookCategory != null){
            String categoryName = bookCategory.getName();
            tagVO.setCategoryName(categoryName);
        }
        return tagVO;
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
        Tag oldTag = tagAdminMapper.selectById(id);
        if (oldTag == null) {
            throw new BaseException(BusinessConstant.TAG_NAME_NOTEXIST);
        }
        Tag tag = ConvertUtils.convert(bookTagDTO, Tag.class);
        tag.setUpdateTime(LocalDateTime.now());
        tag.setId(id);
        int num = tagAdminMapper.updateById(tag);
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
        Long count = bookTagAdminMapper.selectCount(
                Wrappers.<BookTagRelation>lambdaQuery().eq(BookTagRelation::getTagId, id)
        );
        if(count>0){
            throw new BaseException(BusinessConstant.TAG_WITH_BOOKS);
        }
        int num = tagAdminMapper.deleteById(id);
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
        List<Long> usedTagIds = bookTagAdminMapper.selectUsedTagIds(idList);

        if (!usedTagIds.isEmpty()) {
            throw new BaseException(BusinessConstant.TAG_WITH_BOOKS + ":" + usedTagIds);
        }
        int num = tagAdminMapper.deleteByIds(idList);
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
    public List<TagVO> getByCategoryId(Long categoryId) {
        if(categoryId==null || categoryId < 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        LambdaQueryWrapper<Tag> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Tag::getCategoryId, categoryId);

        List<TagVO> tagVOList = tagAdminMapper.selectList(queryWrapper).stream()
                .map(bookTag -> ConvertUtils.convert(bookTag, TagVO.class))
                .collect(Collectors.toList());
        if(tagVOList.isEmpty()){
            throw new BaseException(BusinessConstant.CATEGROY_WITH_TAGS);
        }

        return tagVOList;
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
        LambdaQueryWrapper<Tag> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Tag::getName, name);
        Long count = tagAdminMapper.selectCount(queryWrapper);
        return count>0;
    }


}
