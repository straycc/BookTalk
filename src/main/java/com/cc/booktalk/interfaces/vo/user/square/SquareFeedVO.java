package com.cc.booktalk.interfaces.vo.user.square;

import com.cc.booktalk.interfaces.vo.user.tag.TagVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SquareFeedVO {

    private Long id;

    private String type;

    private String title;

    private String contentSummary;

    private Long authorId;

    private String authorName;

    private String authorAvatar;

    private String cover;

    private Long relatedBookId;

    private String relatedBookName;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    private Double hotScore;

    private LocalDateTime lastActiveTime;

    private LocalDateTime createTime;

    private List<TagVO> tags;
}
