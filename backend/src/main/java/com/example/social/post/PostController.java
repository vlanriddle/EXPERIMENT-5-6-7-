package com.example.social.post;

import com.example.social.common.api.ApiResponse;
import com.example.social.common.api.PageResult;
import com.example.social.user.User;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<PostDtos.PostResponse>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) PostStatus status) {
        return ResponseEntity.ok(ApiResponse.success(200, "Posts loaded",
                postService.list(user.getId(), page, size, sortBy, sortDir, status)));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<PostDtos.PostResponse>>> trending(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(200, "Trending posts loaded", postService.trending(limit)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDtos.PostResponse>> get(
            @AuthenticationPrincipal User user, @PathVariable UUID postId) {
        return ResponseEntity.ok(ApiResponse.success(200, "Post loaded", postService.get(user.getId(), postId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDtos.PostResponse>> create(
            @AuthenticationPrincipal User user, @Valid @RequestBody PostDtos.PostRequest request) {
        PostDtos.PostResponse created = postService.create(user, request);
        return ResponseEntity.created(URI.create("/api/posts/" + created.id()))
                .body(ApiResponse.success(201, "Post created", created));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDtos.PostResponse>> update(
            @AuthenticationPrincipal User user, @PathVariable UUID postId,
            @Valid @RequestBody PostDtos.PostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(200, "Post updated",
                postService.update(user.getId(), postId, request)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user, @PathVariable UUID postId) {
        postService.delete(user.getId(), postId);
        return ResponseEntity.ok(ApiResponse.success(200, "Post deleted", null));
    }
}
