package com.cc.talkserver.user.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.talkcommon.constant.BusinessConstant;
import com.cc.talkcommon.constant.ElasticsearchConstant;
import com.cc.talkcommon.context.UserContext;
import com.cc.talkcommon.exception.BaseException;
import com.cc.talkcommon.utils.ConvertUtils;
import com.cc.talkpojo.dto.TagDTO;
import com.cc.talkpojo.entity.BookCategory;
import com.cc.talkpojo.entity.Tag;
import com.cc.talkpojo.entity.TagES;
import com.cc.talkpojo.vo.TagVO;
import com.cc.talkserver.user.mapper.CategoryUserMapper;
import com.cc.talkserver.user.mapper.TagUserMapper;
import com.cc.talkserver.user.service.TagUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import springfox.documentation.service.Response;

import javax.annotation.Resource;
import java.beans.Transient;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 标签表 服务实现类
 * </p>
 *
 * @author cc
 * @since 2025-08-02
 */
@Service
@Slf4j
public class TagUserServiceImpl extends ServiceImpl<TagUserMapper, Tag> implements TagUserService {

    @Resource
    private TagUserMapper tagUserMapper;

    @Resource
    private CategoryUserMapper categoryUserMapper;

    @Resource
    private ElasticsearchClient elasticsearchClient;

    /**
     * 根据分类id查询标签
     *
     * @param categoryId
     * @return
     */
    @Override
    public List<TagVO> findByCategoryId(Long categoryId) {

        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getCategoryId, categoryId);
        List<Tag> tags = tagUserMapper.selectList(queryWrapper);
        return fillAndConvert(tags);
    }

    /**
     * 查询标签详情
     *
     * @param tagId
     * @return
     */
    @Override
    public TagVO getTagDetail(Long tagId) {
        TagVO tagVO = ConvertUtils.convert(tagUserMapper.selectById(tagId), TagVO.class);
        if (tagVO == null) {
            throw new BaseException(BusinessConstant.TAG_NOT_EXIST);
        }
        return fillCategoryName(tagVO);
    }

    /**
     * 建立新标签
     *
     * @param tagDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void creatNewTag(TagDTO tagDTO) {
        if (tagDTO.getName() == null || tagDTO.getName().trim().equals("")) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 写入mysql
        Tag tag = ConvertUtils.convert(tagDTO, Tag.class);
        tag.setCreatorId(UserContext.getUser().getId());
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        int res = tagUserMapper.insert(tag);
        if (res != 1) {
            throw new BaseException(BusinessConstant.TAG_CREAT_ERROR);
        }

        // 写入ES
        try {
            TagES tagES = ConvertUtils.convert(tagDTO, TagES.class);
            IndexRequest<TagES> indexRequest = IndexRequest.of(
                    idx -> idx.index(ElasticsearchConstant.ES_TAG_INDEX)
                            .id(tagES.getId().toString())
                            .document(tagES)
            );

            IndexResponse response = elasticsearchClient.index(indexRequest);
            log.info("ES更新成功，索引={}，ID={}", response.index(),response.id());
        } catch (Exception e) {
            throw new BaseException(BusinessConstant.TAG_INSERT_ERROR);
        }


    }

    /**
     * 用户删除标签
     *
     * @param tagId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTagById(Long tagId) {
        if (tagId == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }

        // 查询标签
        Tag tag = tagUserMapper.selectById(tagId);
        if (tag == null) {
            throw new BaseException(BusinessConstant.TAG_NOT_EXIST);  // 标签不存在
        }

        // 检查是否为标签创建者或管理员
        Long currentUserId = UserContext.getUser().getId();
        String currentUserRole = UserContext.getUser().getRole();

        if (!Objects.equals(tag.getCreatorId(), currentUserId) &&
                !Objects.equals(currentUserRole, BusinessConstant.USER_ROLE_ADMIN)) {
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR);  // 无删除权限
        }

        int res = tagUserMapper.deleteById(tagId);
        if (res != 1) {
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR);
        }

        // 删除ES中数据
        DeleteRequest deleteRequest = DeleteRequest.of(d -> d
                .index(ElasticsearchConstant.ES_TAG_INDEX)
                .id(tagId.toString())
        );
        try {
            DeleteResponse deleteResponse = elasticsearchClient.delete(deleteRequest);
            if (deleteResponse.result() != Result.Deleted && deleteResponse.result() != Result.NotFound) {
                log.warn("ES删除tag失败，tagId={}，结果={}", tagId, deleteResponse.result());
            }
        } catch (IOException e) {
            log.error("ES删除tag异常，tagId={}", tagId, e);
            throw new BaseException(BusinessConstant.TAG_DELETE_ERROR);
        }
    }

    /**
     * 用户修改标签
     *
     * @param tagDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(TagDTO tagDTO) {
        if (tagDTO.getId() == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        // 检查tag是否存在
        Tag tag = tagUserMapper.selectById(tagDTO.getId());
        if (tag == null) {
            throw new BaseException(BusinessConstant.TAG_NOT_EXIST);
        }
        //检查是否为自己创建的标签
        if (!UserContext.getUser().getId().equals(tag.getCreatorId())
                && !Objects.equals(BusinessConstant.USER_ROLE_ADMIN, UserContext.getUser().getRole())) {
            throw new BaseException(BusinessConstant.WITH_NO_AUTHORITION);
        }
        // 更新tag内容
        tag.setName(tagDTO.getName());
        tag.setDescription(tagDTO.getDescription());
        tag.setCategoryId(tagDTO.getCategoryId());
        tag.setUpdateTime(LocalDateTime.now());

        int res = tagUserMapper.updateById(tag);
        if (res != 1) {
            throw new BaseException(BusinessConstant.TAG_UPDATE_ERROR);
        }

        // 修改ES中数据
        TagES tagES = ConvertUtils.convert(tag, TagES.class);
        IndexRequest<TagES> indexRequest = IndexRequest.of(
                idx -> idx.index(ElasticsearchConstant.ES_TAG_INDEX)
                        .id(tagES.getId().toString())
                        .document(tagES)
        );

        try {
            elasticsearchClient.index(indexRequest);
        } catch (IOException e) {
            log.error("ES更新失败，tagId={}", tagES.getId(), e);
            throw new BaseException(BusinessConstant.TAG_UPDATE_ERROR);
        }

    }

    /**
     * 查询用户创建的所有标签
     * @param userId
     * @return
     */
    @Override
    public List<TagVO> getUserTags(Long userId) {
        // 参数/权限检查
        if (userId == null) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        if (!UserContext.getUser().getId().equals(userId) &&
                !Objects.equals(UserContext.getUser().getRole(), BusinessConstant.USER_ROLE_ADMIN)) {
            throw new BaseException(BusinessConstant.WITH_NO_AUTHORITION);
        }

        // 查询
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getCreatorId, userId);
        List<Tag> tags = tagUserMapper.selectList(queryWrapper);
        return fillAndConvert(tags);
    }


    /**
     * 填充单个分类名
     */
    public TagVO fillCategoryName(TagVO tagVO) {
        if (tagVO.getCategoryId() != null) {
            // 查询分类名称
            BookCategory category = categoryUserMapper.selectById(tagVO.getCategoryId());

            // 如果分类未找到，则抛出异常
            if (category != null) {
                tagVO.setCategoryName(category.getName());  // 设置 categoryName
            } else {
                throw new BaseException(BusinessConstant.CATEGORY_NOT_EXIST);
            }
        }
        return tagVO;
    }

    /**
     * 批量填充分类名，并转换VO
     */
    public List<TagVO> fillAndConvert(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<TagVO>();
        }
        // 填充分类名，并转换VO
        List<Long> categoryIds = tags.stream().map(Tag::getCategoryId).distinct().collect(Collectors.toList());
        Map<Long, String> categoryMap = categoryUserMapper.getCategoryNames(categoryIds); // 批量获取分类名

        return tags.stream().map(tag -> {
            TagVO tagVO = ConvertUtils.convert(tag, TagVO.class);
            tagVO.setCategoryName(categoryMap.get(tag.getCategoryId()));
            return tagVO;
        }).collect(Collectors.toList());
    }

}
