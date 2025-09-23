package com.cc.talkpojo.dto;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BookListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookListId;
    /**
     * 创建者ID
     */
    private Long userId;

    /**
     * 书单标题
     */
    private String title;

    /**
     * 书单简介
     */
    private String description;

    /**
     * 封面图（可选）
     */
    private String coverUrl;

    /**
     * 是否公开（1公开/0私密）
     */
    private Integer visibility;

    /**
     * 书单id列表
     */
    private List<Long> bookIdList;


    /**
     * 单个书籍id
     */
    private Long bookId;
}
