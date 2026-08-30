package com.spring.blog.controller;

import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.blog.dto.PostResponse;
import com.spring.blog.model.Post;
import com.spring.blog.model.TagModel;
import com.spring.blog.service.TagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tags")
public class TagController {
    private final TagService tagService;

    @PostMapping("/posts/{slug}/tags")
    @Operation(summary = "Add tags to post")
    public Post addTagsToPost(@PathVariable String slug, @RequestBody Set<String> tagName) {
        return tagService.addTagsToPost(slug, tagName);
    }

    @GetMapping("/posts/{slug}/tags")
    @Operation(summary = "Get tags of posts")
    public Set<TagModel> getTagsByPost(@PathVariable String slug) {
        return tagService.getTagsByPost(slug);
    }

    @GetMapping("/tags/{tagName}/posts")
    @Operation(summary = "Get posts by tags")
    public List<PostResponse> getPostsByTag(@PathVariable String tagName) {
        return tagService.getPostsByTag(tagName);
    }
}
