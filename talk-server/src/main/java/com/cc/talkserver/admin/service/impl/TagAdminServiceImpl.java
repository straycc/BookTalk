package com.cc.talkserver.admin.service.impl;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.ElasticsearchConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.CheckPageParam;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.result.PageResult;
import com.cc.talkpojo.result.TagUpResult;
import com.cc.talkpojo.dto.PageTagDTO;
import com.cc.talkpojo.dto.TagDTO;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.entity.BookTagRelation;
import com.cc.talkpojo.entity.Tag;
import com.cc.talkpojo.entity.TagES;
import com.cc.talkpojo.vo.TagVO;
import com.cc.talkserver.admin.mapper.BookTagAdminMapper;
import com.cc.talkserver.admin.mapper.CategoryAdminMapper;
import com.cc.talkserver.admin.mapper.TagAdminMapper;
import com.cc.talkserver.admin.service.TagAdminService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
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
@Slf4j
public class TagAdminServiceImpl extends ServiceImpl<TagAdminMapper, Tag> implements TagAdminService {



    @Resource
    private CategoryAdminMapper categoryAdminMapper;

    @Resource
    private TagAdminMapper tagAdminMapper;

    @Resource
    private BookTagAdminMapper bookTagAdminMapper;

    @Resource
    private ElasticsearchClient elasticsearchClient;





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
    @Transactional(rollbackFor = Exception.class)
    public void tagSignalAdd(TagDTO bookTagDTO) {

        if(bookTagDTO == null || bookTagDTO.getName() == null ||  bookTagDTO.getName().isEmpty()){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 检查标签是否已存在
        if(checkTagExist(bookTagDTO.getName())){
            throw new BaseException(BusinessConstant.TAG_NAME_REPEAT);
        }

        // 写入Mysql
        Tag tag = ConvertUtils.convert(bookTagDTO, Tag.class);
        tag.setCreatorId(UserContext.getUser().getId());
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        tagAdminMapper.insert(tag);


        // 写入Elasticsearch
        TagES tagES = ConvertUtils.convert(tag, TagES.class);
        IndexRequest<TagES> indexRequest = IndexRequest.of(
                idx -> idx.index(ElasticsearchConstant.ES_TAG_INDEX)
                        .id(tagES.getId().toString())
                        .document(tagES)
        );
        try {
            IndexResponse response = elasticsearchClient.index(indexRequest);
            log.info("ES更新成功，索引={}，ID={}", response.index(),response.id());
        } catch (IOException e) {
            log.error("ES写入失败",e);
            throw new BaseException(ElasticsearchConstant.ES_WRITE_ERROR);
        }


    }

    /**
     * 批量新增标签
     * @param bookTagsList
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagUpResult tagsBatchAdd(List<TagDTO> bookTagsList) {
        if(bookTagsList == null || bookTagsList.isEmpty()){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        List<String> existTagList = new ArrayList<>();
        List<Tag> saveTagList = new ArrayList<>();
        List<String> emptyTagList = new ArrayList<>();

        for(TagDTO bookTagDTO : bookTagsList){
            String tagName = bookTagDTO.getName();
            //检查标签名是否为空
            if( tagName == null || tagName.isEmpty()){
                emptyTagList.add("标签描述:"+bookTagDTO.getDescription());
                continue;
            }
            // 检查标签是否已存在
            if(checkTagExist(tagName)){
                existTagList.add(tagName);
                continue;
            }

            Tag tag = ConvertUtils.convert(bookTagDTO, Tag.class);

            Long creatorId = UserContext.getUser() != null ? UserContext.getUser().getId() : BusinessConstant.USER_ROLE_ADMIN_ID;
            tag.setCreatorId(creatorId);
            tag.setCreateTime(LocalDateTime.now());
            tag.setUpdateTime(LocalDateTime.now());
            saveTagList.add(tag);
        }

        if(saveTagList.isEmpty()){
            return new TagUpResult(0, emptyTagList, existTagList);
        }

        boolean flag = saveBatch(saveTagList);
        if(!flag){
            throw new BaseException(BusinessConstant.TAG_INSERT_ERROR);
        }

        // ES批量写入
        try {

            List<TagES> tagESList = saveTagList.stream()
                    .map(tag -> ConvertUtils.convert(tag, TagES.class))
                    .collect(Collectors.toList());

            BulkRequest.Builder br = new BulkRequest.Builder();

            for (TagES tagES : tagESList) {
                br.operations(op -> op
                        .index(idx -> idx
                                .index(ElasticsearchConstant.ES_TAG_INDEX)
                                .id(tagES.getId().toString())
                                .document(tagES)
                        )
                );
            }

            BulkResponse result = elasticsearchClient.bulk(br.build());


            if (result.errors()) {
                log.warn("部分标签同步到 ES 失败:");
                result.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> log.warn("写入失败 -> ID: {}, 原因: {}", item.id(), item.error().reason()));
            }
            else{
                log.info("成功同步 {} 个标签到 Elasticsearch", saveTagList.size());
            }

        } catch (Exception e) {
            throw new BaseException(BusinessConstant.TAG_INSERT_ERROR);
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
    @Transactional(rollbackFor = Exception.class)
    public void tagRevise(Long id, TagDTO bookTagDTO) {
        if (id == null || id < 0 || bookTagDTO == null || bookTagDTO.getId() == null || !bookTagDTO.getId().equals(id)) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 查询标签是否存在
        Tag oldTag = tagAdminMapper.selectById(id);
        if (oldTag == null) {
            throw new BaseException(BusinessConstant.TAG_NAME_NOTEXIST);
        }

        // 修改mysql
        Tag tag = ConvertUtils.convert(bookTagDTO, Tag.class);
        tag.setUpdateTime(LocalDateTime.now());
        tag.setId(id);
        int num = tagAdminMapper.updateById(tag);
        if (num != 1) {
            throw new BaseException(BusinessConstant.TAG_UPDATE_ERROR);
        }

        // 修改ES
        TagES tagES = ConvertUtils.convert(tag, TagES.class);
        IndexRequest<TagES> indexRequest = IndexRequest.of(
                idx -> idx.index(ElasticsearchConstant.ES_TAG_INDEX)
                        .id(tagES.getId().toString())
                        .document(tagES)
        );

        try {
            elasticsearchClient.index(indexRequest);
        } catch (IOException e) {
            log.error("ES更新失败，tagId={}", id, e);
            throw new BaseException(BusinessConstant.TAG_UPDATE_ERROR);
        }
    }


    /**
     * 删除单个标签
     * @param id
     */
    @Override
    public void tagSignalDelete(Long id) {

        // 检查用户权限
        if(!UserContext.getUser().getRole().equals(BusinessConstant.USER_ROLE_ADMIN)){
            throw new BaseException(BusinessConstant.WITH_NO_AUTHORITION);
        }

        if(id==null || id < 0){
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        Long count = bookTagAdminMapper.selectCount(
                Wrappers.<BookTagRelation>lambdaQuery().eq(BookTagRelation::getTagId, id)
        );
        if(count>0){
            throw new BaseException(BusinessConstant.TAG_WITH_BOOKS);
        }

        // 删除数据库中数据
        int num = tagAdminMapper.deleteById(id);
        if(num != 1){
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR);
        }

        // 删除ES中数据
        DeleteRequest deleteRequest = DeleteRequest.of(d -> d
                .index(ElasticsearchConstant.ES_TAG_INDEX)
                .id(id.toString())
        );
        try {
            DeleteResponse deleteResponse = elasticsearchClient.delete(deleteRequest);
            if (deleteResponse.result() != Result.Deleted && deleteResponse.result() != Result.NotFound) {
                log.warn("ES删除tag失败，tagId={}，结果={}", id, deleteResponse.result());
            }
        } catch (IOException e) {
            log.error("ES删除tag异常，tagId={}", id, e);
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

        // 删除数据库中数据
        int num = tagAdminMapper.deleteByIds(idList);
        if(num == 0){
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR + ":" + idList);
        }

        // 删除ES中数据
        try {
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

            for (Long id : idList) {
                bulkRequestBuilder.operations(op -> op
                        .delete(d -> d
                                .index(ElasticsearchConstant.ES_BOOK_INDEX)
                                .id(id.toString())
                        )
                );
            }

            BulkResponse response = elasticsearchClient.bulk(bulkRequestBuilder.build());

            if (response.errors()) {
                log.error("部分 ES 删除失败：");
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> log.error("失败ID: {}, 原因: {}", item.id(), item.error().reason()));

                throw new BaseException(BusinessConstant.TAG_DELETE_ERROR);
            } else {
                log.info("成功删除 {} 条数据（ES）", idList.size());
            }

        } catch (Exception e) {
            log.error("ES 批量删除失败", e);
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR);
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


    /**
     * 检查用户权限
     * @return
     */
    public void checkAuthoration(){
        // 检查用户权限
        if(!UserContext.getUser().getRole().equals(BusinessConstant.USER_ROLE_ADMIN)){
            throw new BaseException(BusinessConstant.WITH_NO_AUTHORITION);
        }
    }

}
