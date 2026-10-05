package com.example.social.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

public final class PostDtos {
    private PostDtos() {}

    public record PostRequest(
            @NotBlank @Size(max = 2000) String content,
            @Size(max = 2048) @URL(protocol = "https", host = "") String mediaUrl,
            PostStatus status,
            Instant scheduledAt) {}

    public record PostResponse(
            UUID id,
            String content,
            String mediaUrl,
            PostStatus status,
            Instant scheduledAt,
            UUID authorId,
            String authorName,
            Instant createdAt,
            Instant updatedAt) {
        public static PostResponse from(Post post) {
            return new PostResponse(post.getId(), post.getContent(), post.getMediaUrl(), post.getStatus(),
                    post.getScheduledAt(), post.getAuthor().getId(), post.getAuthor().getDisplayName(),
                    post.getCreatedAt(), post.getUpdatedAt());
        }
    }
}
