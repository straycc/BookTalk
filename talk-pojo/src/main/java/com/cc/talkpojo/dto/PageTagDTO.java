package com.cc.talkpojo.dto;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PageTagDTO implements PageDTO{

    /**
     * 页面号
     */
    private Integer pageNum;

    /**
     * 页面大小
     */
    private Integer pageSize;

    /**
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 起始时间
     */
    private LocalDate createdFrom;

    /**
     * 结束时间
     */
    private LocalDate createdTo;

}
