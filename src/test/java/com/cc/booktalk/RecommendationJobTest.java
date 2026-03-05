package com.cc.booktalk;

import com.cc.booktalk.interfaces.schedule.RecommendationJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "booktalk.websocket.enabled=false"
)
public class RecommendationJobTest {


    @Autowired
    private RecommendationJob recommendationJob;

    @Test
    public void hotRecommendationJobTest() {
        recommendationJob.updateHotRecommendations();
    }


}
