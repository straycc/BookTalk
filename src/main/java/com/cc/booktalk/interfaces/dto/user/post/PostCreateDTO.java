package com.cc.booktalk.interfaces.dto.user.post;

import lombok.Data;

import java.util.List;

@Data
public class PostCreateDTO {

    private String title;

    private String content;

    private String summary;

    private Long relatedBookId;

    private List<Long> tagIds;
}
