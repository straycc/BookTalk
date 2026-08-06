package com.cc.booktalk.interfaces.vo.user.post;

import com.cc.booktalk.interfaces.vo.user.tag.TagVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostVO {

    private Long id;

    private Long userId;

    private String authorName;

    private String authorAvatar;

    private String title;

    private String content;

    private String summary;

    private Long relatedBookId;

    private String relatedBookName;

    private String relatedBookCover;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Double hotScore;

    private LocalDateTime lastActiveTime;

    private LocalDateTime createTime;

    private List<TagVO> tags;
}
