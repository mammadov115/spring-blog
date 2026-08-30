package com.spring.blog.dto;

import java.util.List;

public record KeysetPostResponse(
        List<PostResponse> content,
        boolean hasNext,
        Long nextCursor) {
}