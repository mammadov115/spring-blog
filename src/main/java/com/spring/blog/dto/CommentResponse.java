package com.spring.blog.dto;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id, 
    String name, 
    String email, 
    String body, 
    LocalDateTime created
){}
