package com.cc.talkpojo.Result;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryUpResult {

    /**
     * 成功插入的数量
     */
    private int successCount;

    /**
     *  已经存在分类
     */
    private List<String> existCategoryList;

    /**
     * 分类名为空的分类
     */
    private List<String> emptyCategoryList;
}
