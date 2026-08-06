package com.cc.booktalk.content;

import com.cc.booktalk.application.user.service.content.impl.ContentTagServiceImpl;
import com.cc.booktalk.domain.entity.tag.ContentTagRelation;
import com.cc.booktalk.domain.entity.tag.Tag;
import com.cc.booktalk.infrastructure.persistence.user.mapper.tag.ContentTagRelationMapper;
import com.cc.booktalk.infrastructure.persistence.user.mapper.tag.TagUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentTagServiceImplTest {

    @InjectMocks
    private ContentTagServiceImpl contentTagService;

    @Mock
    private ContentTagRelationMapper contentTagRelationMapper;

    @Mock
    private TagUserMapper tagUserMapper;

    @Test
    void replaceContentTagsUpdatesRelationsAndUsageCounts() {
        when(tagUserMapper.selectBatchIds(List.of(2L))).thenReturn(List.of(tag(2L)));
        when(contentTagRelationMapper.selectList(any())).thenReturn(List.of(relation(1L)));

        contentTagService.replaceContentTags(10L, "POST", List.of(2L));

        verify(contentTagRelationMapper).delete(any());
        verify(contentTagRelationMapper).insert(any(ContentTagRelation.class));
        verify(tagUserMapper, times(2)).update(isNull(), any());
    }

    @Test
    void deleteContentTagsDecrementsUsageCount() {
        when(contentTagRelationMapper.selectList(any())).thenReturn(List.of(relation(1L)));

        contentTagService.deleteContentTags(10L, "REVIEW");

        verify(contentTagRelationMapper).delete(any());
        verify(tagUserMapper).update(isNull(), any());
    }

    private Tag tag(Long id) {
        Tag tag = new Tag();
        tag.setId(id);
        return tag;
    }

    private ContentTagRelation relation(Long tagId) {
        return ContentTagRelation.builder().contentId(10L).contentType("POST").tagId(tagId).build();
    }
}
