package com.spring.blog.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.blog.model.Post;
import com.spring.blog.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("api/posts")
@Tag(name = "posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping("/{slug}")
    @Operation(summary = "Retrieve a post by slug")
    public Post getPostBySlug(@PathVariable String slug) {
        return postService.getPostBySlug(slug);
    }

    @GetMapping
    @Operation(summary = "List published all posts by pageable")
    public Page<Post> getAllPosts(@ParameterObject Pageable pageable) {
        return postService.getPosts(pageable);
    }
}