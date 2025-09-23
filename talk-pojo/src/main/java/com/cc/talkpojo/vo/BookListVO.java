package com.cc.talkpojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
public class BookListVO implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 创建者昵称
     */
    private String nickName;

    /**
     * 书单标题
     */
    private String title;

    /**
     * 封面图（可选）
     */
    private String coverUrl;

}
