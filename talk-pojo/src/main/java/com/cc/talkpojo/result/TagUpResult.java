package com.cc.talkpojo.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagUpResult {

    /**
     * 成功插入的数量
     */
    private int successCount;

    /**
     *  已经存在标签
     */
    private List<String> existTagList;

    /**
     * 标签名为空
     */
    private List<String> emptyTagList;
}
