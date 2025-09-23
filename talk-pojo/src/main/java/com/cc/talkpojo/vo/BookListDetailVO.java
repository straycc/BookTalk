package com.cc.talkpojo.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class BookListDetailVO {

    /**
     * 书单title
     */
    private String title;
    /**
     * 书单描述
     */
    private String description;
    /**
     * 书单封面
     */
    private String coverUrl;

    /**
     * 书单创建者
     */
    private String nickName;

    /**
     * 书单创建者头像
     */
    private String avatar;

    /**
     * 书单包含书籍信息
     */
    private List<BookShowVO> bookShows;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
