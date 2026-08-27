package com.spring.blog.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.spring.blog.model.Post;
import com.spring.blog.model.Status;
import com.spring.blog.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public List<Post> getPosts() {
        return postRepository.findByStatusOrderByPublishDesc(Status.PUBLISHED);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public Post getPostBySlug(String slug) {
        return postRepository.findBySlugAndStatus(slug, Status.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public Page<Post> getPosts(Pageable pageable) {
        return postRepository.findByStatusOrderByPublishDesc(Status.PUBLISHED, pageable);
    }
}
