package com.spring.blog.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String body;
    private String authorName;
    private LocalDateTime publish;
    private String status;
}
