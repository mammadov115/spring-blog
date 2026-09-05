package com.spring.blog.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.blog.dto.KeysetPostResponse;
import com.spring.blog.dto.PostRequest;
import com.spring.blog.dto.PostResponse;
import com.spring.blog.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import com.spring.blog.exception.ResourceNotFoundException;
import com.spring.blog.model.Post;
import com.spring.blog.model.Status;
import com.spring.blog.model.TagModel;
import com.spring.blog.repository.PostRepository;
import com.spring.blog.util.PostMapper;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    private static String stripNullBytes(String s) {
        if (s == null)
            return null;
        return s.replaceAll("\u0000", "");
    }

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

    @Cacheable(value = "post", key = "#slug")
    @Transactional(readOnly = true)
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + slug));
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findByStatus(Status.PUBLISHED, pageable)
                .map(this::toResponse);
    }

    @Cacheable(value = "similar", key = "#slug")
    @Transactional(readOnly = true)
    public List<PostResponse> getSimilarPosts(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found:" + slug));

        if (post.getTags().isEmpty()) {
            return List.of();
        }

        List<Post> similarPosts = postRepository.findSimilarPosts(post.getId(), Status.PUBLISHED,
                PageRequest.of(0, 4));

        return similarPosts.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Cacheable(value = "search", key = "#query")
    @Transactional(readOnly = true)
    public List<PostResponse> searchPosts(String query) {
        List<Long> ids = postRepository.fullTextSearchIds(query);
        if (ids.isEmpty())
            return List.of();

        // ID sıralamasını qorumaq üçün (rank sırası pozulmasın)
        List<Post> posts = postRepository.findByIds(ids);
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        return ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(postMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KeysetPostResponse getPostsKeyset(Long cursor, int size) {
        List<Post> posts = postRepository.findByStatusKeyset(Status.PUBLISHED, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = posts.size() > size;

        if (hasNext) {
            posts = new ArrayList<>(posts.subList(0, size));
        }

        List<PostResponse> content = posts.stream().map(this::toResponse).collect(Collectors.toList());
        Long nextCursor = hasNext ? posts.get(posts.size() - 1).getId() : null;

        return new KeysetPostResponse(content, hasNext, nextCursor);
    }

    @Transactional
    public PostResponse createPost(PostRequest request) {
        String rawTitle = stripNullBytes(request.title());
        String slug = (request.slug() == null || request.slug().isBlank())
                ? generateSlug(rawTitle)
                : stripNullBytes(request.slug());

        if (postRepository.existsBySlug(slug)) {
            throw new ConflictException("Slug already exists: " + slug);
        }

        Post post = new Post();
        post.setTitle(rawTitle);
        post.setSlug(slug);
        post.setBody(Jsoup.clean(request.body(), Safelist.basicWithImages()));
        post.setStatus(request.status());
        post.setPublish(LocalDateTime.now());

        try {
            return toResponse(postRepository.save(post));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Slug already exists: " + slug);
        }
    }

    @Caching(put = {
            @CachePut(value = "post", key = "#slug")
    }, evict = {
            @CacheEvict(value = "similar", key = "#slug")
    })
    @Transactional
    public PostResponse updatePost(String slug, PostRequest request) {
        String rawTitle = stripNullBytes(request.title());

        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + slug));

        post.setTitle(rawTitle);
        post.setBody(Jsoup.clean(request.body(), Safelist.basicWithImages()));
        post.setStatus(request.status());

        return toResponse(postRepository.save(post));
    }

    @Caching(evict = {
            @CacheEvict(value = "post", key = "#slug"),
            @CacheEvict(value = "similar", key = "#slug")
    })
    @Transactional
    public void deletePost(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found " + slug));
        postRepository.delete(post);
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}
