package com.example.social.security;

import com.example.social.common.api.ApiResponse;
import com.example.social.post.PostRepository;
import com.example.social.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final PostRepository posts;

    public AdminController(UserRepository users, PostRepository posts) {
        this.users = users;
        this.posts = posts;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminOverview>> overview() {
        AdminOverview overview = new AdminOverview(users.count(), posts.count());
        return ResponseEntity.ok(ApiResponse.success(200, "Admin overview loaded", overview));
    }

    public record AdminOverview(long users, long posts) {}
}
