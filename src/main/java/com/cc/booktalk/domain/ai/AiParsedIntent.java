package com.cc.booktalk.domain.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户自然语言推荐意图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParsedIntent {

    private String intent;

    @Builder.Default
    private List<String> themes = new ArrayList<>();

    @Builder.Default
    private List<String> preferredAuthors = new ArrayList<>();

    @Builder.Default
    private List<String> preferredCategories = new ArrayList<>();

    @Builder.Default
    private List<String> constraints = new ArrayList<>();

    @Builder.Default
    private List<String> exclude = new ArrayList<>();

    private String tone;

    private String difficulty;

    /**
     * 原始问题摘要，便于后续追问复用。
     */
    private String querySummary;
}
