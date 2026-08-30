package com.spring.blog.util;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.spring.blog.dto.PostResponse;
import com.spring.blog.model.Post;
import com.spring.blog.model.TagModel;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post) {
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
                        .collect(Collectors.toSet())
        );
    }
}