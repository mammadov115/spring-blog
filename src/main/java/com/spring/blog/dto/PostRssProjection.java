package com.spring.blog.dto;

import java.time.LocalDateTime;

public interface PostRssProjection {
    String getTitle();
    String getSlug();
    String getBody();
    LocalDateTime getPublish();
}
