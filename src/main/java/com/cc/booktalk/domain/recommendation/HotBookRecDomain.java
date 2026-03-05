package com.cc.booktalk.domain.recommendation;


import com.cc.booktalk.domain.entity.recommendation.UserBehaviorLog;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Service
public class HotBookRecDomain {

    // 行为权重
    private static final Map<String, Double> WEIGHTS = Map.of(
            "BOOK_COLLECT", 5.0,
            "BOOK_REVIEW", 4.0,
            "BOOK_LIKE", 3.0,
            "BOOK_VIEW", 0.5
    );

    private static final double DEFAULT_WEIGHT = 1.0;

    // 7天半衰期：lambda = ln(2)/7
    private static final double LAMBDA = Math.log(2.0) / 7.0;

    // 最低行为门槛（用于过滤噪声）
    private static final int MIN_ACTIONS = 3;

    public boolean enoughActions(int actionCount) {
        return actionCount >= MIN_ACTIONS;
    }

    //TODO: 用户行为分数采用循环计算，行为数据量大时对CPU消耗大
    //待优化：通过SQL聚合优化，提前做预处理
    public double calculateHotScore(List<UserBehaviorLog> behaviors, LocalDateTime now) {
        if (behaviors == null || behaviors.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        for (UserBehaviorLog b : behaviors) {
            // 1. 获取基础权重
            double baseWeight = WEIGHTS.getOrDefault(b.getBehaviorType(), DEFAULT_WEIGHT);

            // 2. 时间差（使用分钟或小时作为单位更精确，避免天数不足1时 decay 几乎为 1）
            double hoursDiff = Duration.between(b.getCreateTime(), now).toHours();
            double daysDiff = Math.max(0.0, hoursDiff / 24.0);

            // 3. 计算衰减 (指数衰减)
            double decay = Math.exp(-LAMBDA * daysDiff);

            totalScore += baseWeight * decay;
        }

        // 4. 建议：对最终结果进行 Log 平滑，防止极热门书籍的分数变成“天文数字”
         return Math.log10(totalScore + 1);
    }

}
