package com.example.social.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledPostPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledPostPublisher.class);
    private final PostService postService;

    public ScheduledPostPublisher(PostService postService) {
        this.postService = postService;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.publish-interval-ms:30000}")
    public void publishDuePosts() {
        int published = postService.publishDue();
        if (published > 0) {
            LOGGER.info("Published {} scheduled posts", published);
        }
    }
}
