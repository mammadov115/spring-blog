package com.spring.blog.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record PostResponse(
    Long id,
    String title,
    String slug,
    String body,
    String authorName,
    LocalDateTime publish,
    String status,
    Set<String> tags
) {}