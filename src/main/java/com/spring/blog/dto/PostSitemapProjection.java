package com.spring.blog.dto;

import java.time.LocalDateTime;

public interface PostSitemapProjection {
    String getSlug();
    LocalDateTime getUpdated();
}
