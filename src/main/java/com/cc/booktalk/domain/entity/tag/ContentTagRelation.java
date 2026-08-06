package com.cc.booktalk.domain.entity.tag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容标签关联。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("content_tag_relation")
public class ContentTagRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long contentId;

    private String contentType;

    private Long tagId;

    private LocalDateTime createTime;
}
