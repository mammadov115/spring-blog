package com.spring.blog.controller;

import com.spring.blog.service.EmailService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.spring.blog.service.PostService;
import com.spring.blog.dto.KeysetPostResponse;
import com.spring.blog.dto.PostRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

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

    @GetMapping("/keyset")
    @Operation(summary = "List posts with keyset pagination")
    public KeysetPostResponse getPostsKeyset(
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size
    ){
        return postService.getPostsKeyset(cursor, size);
    }

    @PostMapping 
    @Operation(summary = "Create a new post")
    public ResponseEntity<PostResponse> createPost(@RequestBody @Valid PostRequest request){
        return  ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request));
    }

    @PutMapping("/{slug}")
    @Operation(summary = "Update a post by slug")
    public ResponseEntity<PostResponse> updatePost(@PathVariable String slug, @RequestBody @Valid PostRequest request){
        return ResponseEntity.ok(postService.updatePost(slug, request));
    }

    @DeleteMapping("/{slug}")
    @Operation(summary = "Delete a post by slug")
    public ResponseEntity<Void> deletePost(@PathVariable String slug){
        postService.deletePost(slug);
        return ResponseEntity.noContent().build();
    }
}