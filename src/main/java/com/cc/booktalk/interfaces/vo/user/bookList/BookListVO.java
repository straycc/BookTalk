package com.cc.booktalk.interfaces.vo.user.bookList;

import lombok.Data;

import java.io.Serializable;


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
