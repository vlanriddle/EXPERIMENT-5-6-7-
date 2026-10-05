package com.example.social.post;

import com.example.social.common.api.PageResult;
import com.example.social.common.error.ResourceNotFoundException;
import com.example.social.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "status", "scheduledAt");

    private final PostRepository posts;

    public PostService(PostRepository posts) {
        this.posts = posts;
    }

    @Transactional(readOnly = true)
    public PageResult<PostDtos.PostResponse> list(UUID ownerId, int page, int size, String sortBy,
            String sortDir, PostStatus status) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("sortBy must be one of: " + String.join(", ", SORT_FIELDS));
        }
        int boundedSize = Math.max(1, Math.min(size, 50));
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, boundedSize, Sort.by(direction, sortBy));
        Page<Post> result = status == null
                ? posts.findByAuthorId(ownerId, pageable)
                : posts.findByAuthorIdAndStatus(ownerId, status, pageable);
        return PageResult.from(result.map(PostDtos.PostResponse::from));
    }

    @Transactional(readOnly = true)
    public PostDtos.PostResponse get(UUID ownerId, UUID postId) {
        return PostDtos.PostResponse.from(findOwned(ownerId, postId));
    }

    @CacheEvict(cacheNames = "trending", allEntries = true)
    @Transactional
    public PostDtos.PostResponse create(User owner, PostDtos.PostRequest request) {
        PostStatus status = request.status() == null ? PostStatus.PUBLISHED : request.status();
        validateSchedule(status, request.scheduledAt());
        Post post = posts.save(new Post(request.content().trim(), request.mediaUrl(), status,
                status == PostStatus.SCHEDULED ? request.scheduledAt() : null, owner));
        return PostDtos.PostResponse.from(post);
    }

    @CacheEvict(cacheNames = "trending", allEntries = true)
    @Transactional
    public PostDtos.PostResponse update(UUID ownerId, UUID postId, PostDtos.PostRequest request) {
        PostStatus status = request.status() == null ? PostStatus.DRAFT : request.status();
        validateSchedule(status, request.scheduledAt());
        Post post = findOwned(ownerId, postId);
        post.update(request.content().trim(), request.mediaUrl(), status,
                status == PostStatus.SCHEDULED ? request.scheduledAt() : null);
        return PostDtos.PostResponse.from(post);
    }

    @CacheEvict(cacheNames = "trending", allEntries = true)
    @Transactional
    public void delete(UUID ownerId, UUID postId) {
        posts.delete(findOwned(ownerId, postId));
    }

    @Cacheable(cacheNames = "trending", key = "#limit")
    @Transactional(readOnly = true)
    public List<PostDtos.PostResponse> trending(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 20));
        return posts.findRecentPublished(PageRequest.of(0, boundedLimit)).stream()
                .map(PostDtos.PostResponse::from).toList();
    }

    @CacheEvict(cacheNames = "trending", allEntries = true)
    @Transactional
    public int publishDue() {
        return posts.publishDuePosts(Instant.now());
    }

    private Post findOwned(UUID ownerId, UUID postId) {
        return posts.findByIdAndAuthorId(postId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private void validateSchedule(PostStatus status, Instant scheduledAt) {
        if (status == PostStatus.SCHEDULED && (scheduledAt == null || !scheduledAt.isAfter(Instant.now()))) {
            throw new IllegalArgumentException("Scheduled posts require a future scheduledAt timestamp");
        }
    }
}
