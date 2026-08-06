package com.cc.booktalk.application.user.service.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.booktalk.application.user.service.content.ContentTagService;
import com.cc.booktalk.common.constant.BusinessConstant;
import com.cc.booktalk.common.exception.BaseException;
import com.cc.booktalk.domain.entity.tag.ContentTagRelation;
import com.cc.booktalk.domain.entity.tag.Tag;
import com.cc.booktalk.interfaces.vo.user.tag.TagVO;
import com.cc.booktalk.infrastructure.persistence.user.mapper.tag.ContentTagRelationMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.tag.TagUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContentTagServiceImpl implements ContentTagService {

    @Resource
    private ContentTagRelationMapper contentTagRelationMapper;

    @Resource
    private TagUserMapper tagUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceContentTags(Long contentId, String contentType, List<Long> tagIds) {
        if (contentId == null || contentType == null || contentType.isBlank()) {
            return;
        }
        List<Long> normalizedTagIds = normalizeTagIds(tagIds);
        if (!normalizedTagIds.isEmpty()) {
            List<Tag> tags = tagUserMapper.selectBatchIds(normalizedTagIds);
            if (tags.size() != normalizedTagIds.size()) {
                throw new BaseException(BusinessConstant.TAG_NOT_EXIST);
            }
        }

        LambdaQueryWrapper<ContentTagRelation> relationWrapper = new LambdaQueryWrapper<ContentTagRelation>()
                .eq(ContentTagRelation::getContentId, contentId)
                .eq(ContentTagRelation::getContentType, contentType);
        Set<Long> oldTagIds = contentTagRelationMapper.selectList(relationWrapper).stream()
                .map(ContentTagRelation::getTagId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        contentTagRelationMapper.delete(relationWrapper);

        LocalDateTime now = LocalDateTime.now();
        for (Long tagId : normalizedTagIds) {
            contentTagRelationMapper.insert(ContentTagRelation.builder()
                    .contentId(contentId)
                    .contentType(contentType)
                    .tagId(tagId)
                    .createTime(now)
                    .build());
        }
        Set<Long> newTagIds = new LinkedHashSet<>(normalizedTagIds);
        updateUsageCount(difference(newTagIds, oldTagIds), 1);
        updateUsageCount(difference(oldTagIds, newTagIds), -1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContentTags(Long contentId, String contentType) {
        if (contentId == null || contentType == null || contentType.isBlank()) {
            return;
        }
        LambdaQueryWrapper<ContentTagRelation> wrapper = new LambdaQueryWrapper<ContentTagRelation>()
                .eq(ContentTagRelation::getContentId, contentId)
                .eq(ContentTagRelation::getContentType, contentType);
        Set<Long> tagIds = contentTagRelationMapper.selectList(wrapper).stream()
                .map(ContentTagRelation::getTagId)
                .collect(Collectors.toSet());
        contentTagRelationMapper.delete(wrapper);
        updateUsageCount(tagIds, -1);
    }

    @Override
    public List<TagVO> getTagsByContent(Long contentId, String contentType) {
        if (contentId == null) {
            return List.of();
        }
        return getTagsByContents(List.of(contentId), contentType)
                .getOrDefault(contentId, List.of());
    }

    @Override
    public Map<Long, List<TagVO>> getTagsByContents(List<Long> contentIds, String contentType) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ContentTagRelation> relations = contentTagRelationMapper.selectList(
                new LambdaQueryWrapper<ContentTagRelation>()
                        .eq(ContentTagRelation::getContentType, contentType)
                        .in(ContentTagRelation::getContentId, contentIds)
        );
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> tagIdMap = relations.stream()
                .collect(Collectors.groupingBy(ContentTagRelation::getContentId,
                        LinkedHashMap::new,
                        Collectors.mapping(ContentTagRelation::getTagId, Collectors.toList())));

        List<Long> allTagIds = relations.stream()
                .map(ContentTagRelation::getTagId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, TagVO> tagVOMap = tagUserMapper.selectBatchIds(allTagIds).stream()
                .collect(Collectors.toMap(Tag::getId, this::toTagVO));

        Map<Long, List<TagVO>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Long>> entry : tagIdMap.entrySet()) {
            List<TagVO> tags = new ArrayList<>();
            for (Long tagId : entry.getValue()) {
                TagVO tagVO = tagVOMap.get(tagId);
                if (tagVO != null) {
                    tags.add(tagVO);
                }
            }
            result.put(entry.getKey(), tags);
        }
        return result;
    }

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = new LinkedHashSet<>(tagIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList()));
        if (normalized.size() > BusinessConstant.CONTENT_TAG_LIMIT) {
            throw new BaseException(BusinessConstant.PARAM_ERROR);
        }
        return new ArrayList<>(normalized);
    }

    private Set<Long> difference(Set<Long> left, Set<Long> right) {
        Set<Long> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private void updateUsageCount(Set<Long> tagIds, int delta) {
        if (tagIds == null || tagIds.isEmpty() || delta == 0) {
            return;
        }
        String expression = delta > 0
                ? "usage_count = IFNULL(usage_count, 0) + 1"
                : "usage_count = GREATEST(IFNULL(usage_count, 0) - 1, 0)";
        tagUserMapper.update(null, new LambdaUpdateWrapper<Tag>()
                .in(Tag::getId, tagIds)
                .setSql(expression));
    }

    private TagVO toTagVO(Tag tag) {
        TagVO tagVO = new TagVO();
        tagVO.setId(tag.getId());
        tagVO.setCreatorId(tag.getCreatorId());
        tagVO.setCategoryId(tag.getCategoryId());
        tagVO.setName(tag.getName());
        tagVO.setDescription(tag.getDescription());
        tagVO.setUsageCount(tag.getUsageCount());
        tagVO.setCreateTime(tag.getCreateTime());
        tagVO.setUpdateTime(tag.getUpdateTime());
        return tagVO;
    }
}
