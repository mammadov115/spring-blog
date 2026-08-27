package com.spring.blog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spring.blog.model.Post;
import com.spring.blog.model.Status;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStatusOrderByPublishDesc(Status status);

    Optional<Post> findBySlugAndStatus(String slug, Status status);
}
