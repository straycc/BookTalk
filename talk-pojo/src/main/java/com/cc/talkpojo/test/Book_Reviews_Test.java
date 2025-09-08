package com.cc.talkpojo.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;


@Data
public class Book_Reviews_Test {

    @JsonProperty("tag")
    private String tag;

    @JsonProperty("book_name")
    private String bookName;

    @JsonProperty("user_name")
    private String userName;

    private String title;

    // 将 score 字段类型改为 Double，与 JSON 数据匹配
    @JsonProperty("star")
    private Double score;

    @JsonProperty("comment")
    private String content;

    // 将 likeCount 字段类型改为 Double，与 JSON 数据匹配
    @JsonProperty("vote_count")
    private Double likeCount;

    // 映射 JSON 中的 date 字段，并指定日期格式
    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}