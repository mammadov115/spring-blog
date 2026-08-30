package com.spring.blog.controller;

import com.spring.blog.service.EmailService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.spring.blog.service.PostService;
import com.spring.blog.dto.PostResponse;
import com.spring.blog.dto.SharePostRequest;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("api/posts")
@Tag(name = "Posts")
@RequiredArgsConstructor
public class PostController {
    private final EmailService emailService;
    private final PostService postService;

    @GetMapping("/{slug}")
    @Operation(summary = "Retrieve a post by slug")
    public PostResponse getPostBySlug(@PathVariable String slug) {
        return postService.getPostBySlug(slug);
    }

    @GetMapping
    @Operation(summary = "List published all posts by pageable")
    public Page<PostResponse> getAllPosts(@ParameterObject Pageable pageable) {
        return postService.getPosts(pageable);
    }

    @PostMapping("/{slug}/share")
    @Operation(summary = "Share post")
    public ResponseEntity<String> sharePost(@PathVariable String slug, 
                                            @RequestBody @Valid SharePostRequest request) {
        PostResponse post = postService.getPostBySlug(slug);
        emailService.sharePost(post, request);
        return ResponseEntity.ok("Post shared successfully");
    }

    @GetMapping("/{slug}/similar")
    @Operation(summary = "Get similar posts by tags")
    public List<PostResponse> getSimilarPosts(@PathVariable String slug){
        return postService.getSimilarPosts(slug);
    }

    @GetMapping("/search")
    @Operation(summary = "Full text search posts")
    public List<PostResponse> searchPosts(@RequestParam String query){
        return postService.searchPosts(query);
    }
}