package com.cc.booktalk.application.user.service.content;

import com.cc.booktalk.interfaces.vo.user.tag.TagVO;

import java.util.List;
import java.util.Map;

public interface ContentTagService {

    void replaceContentTags(Long contentId, String contentType, List<Long> tagIds);

    void deleteContentTags(Long contentId, String contentType);

    List<TagVO> getTagsByContent(Long contentId, String contentType);

    Map<Long, List<TagVO>> getTagsByContents(List<Long> contentIds, String contentType);
}
