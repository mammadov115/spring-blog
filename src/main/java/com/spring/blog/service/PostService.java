package com.spring.blog.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.blog.dto.PostResponse;
import com.spring.blog.exception.ResourceNotFoundException;
import com.spring.blog.model.Post;
import com.spring.blog.model.Status;
import com.spring.blog.model.TagModel;
import com.spring.blog.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getBody(),
                post.getAuthor() != null ? post.getAuthor().getUsername() : null,
                post.getPublish(),
                post.getStatus().name(),
                post.getTags().stream()
                        .map(TagModel::getName)
                        .collect(Collectors.toSet()));
    }

    public List<Post> getPosts() {
        return postRepository.findByStatusOrderByPublishDesc(Status.PUBLISHED);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    @Transactional(readOnly = true)
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + slug));
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findByStatusOrderByPublishDesc(Status.PUBLISHED, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getSimilarPosts(String slug){
        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED).orElseThrow(()-> new ResourceNotFoundException("Post not found:" + slug));

        if (post.getTags().isEmpty()) {
            return List.of();
        }

        List<Post> similarPosts = postRepository.findSimilaryPosts(post.getTags(), post.getId(), Status.PUBLISHED, PageRequest.of(0, 4));

        return similarPosts.stream().map(this::toResponse).collect(Collectors.toList());

    }


}
