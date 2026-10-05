package com.example.social.post;

import com.example.social.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 2048)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    private Instant scheduledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Post() {}

    public Post(String content, String mediaUrl, PostStatus status, Instant scheduledAt, User author) {
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.author = author;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getContent() { return content; }
    public String getMediaUrl() { return mediaUrl; }
    public PostStatus getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public User getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String content, String mediaUrl, PostStatus status, Instant scheduledAt) {
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.status = status;
        this.scheduledAt = scheduledAt;
    }
}
