package com.example.social.post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @EntityGraph(attributePaths = "author")
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findByAuthorIdAndStatus(UUID authorId, PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Post> findByIdAndAuthorId(UUID id, UUID authorId);

    @Query("select p from Post p join fetch p.author where p.status = com.example.social.post.PostStatus.PUBLISHED order by p.createdAt desc")
    List<Post> findRecentPublished(Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Post p set p.status = com.example.social.post.PostStatus.PUBLISHED, p.updatedAt = :now "
            + "where p.status = com.example.social.post.PostStatus.SCHEDULED and p.scheduledAt <= :now")
    int publishDuePosts(@Param("now") java.time.Instant now);
}
