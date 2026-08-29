package com.spring.blog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.blog.model.Post;
import com.spring.blog.model.Status;
import com.spring.blog.model.TagModel;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStatusOrderByPublishDesc(Status status);

    Optional<Post> findBySlugAndStatus(String slug, Status status);

    Page<Post> findByStatusOrderByPublishDesc(Status status, Pageable pageable);

    List<Post> findByTagsContaining(TagModel tag);
}
