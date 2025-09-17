package com.cc.talkpojo.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResult {
    /**
     * 成功插入的数量
     */
    private int successCount;

    /**
     * 被跳过的图书ISBN
     */
    private List<String> skippedIsbn;

    /**
     * ISBN为空的图书
     */
    private List<String> invalidTitleList;

}