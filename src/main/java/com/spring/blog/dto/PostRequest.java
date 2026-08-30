package com.spring.blog.dto;

import com.spring.blog.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PostRequest(
    @NotBlank(message = "Title is required")
    String title,

    String slug,

    @NotBlank(message = "Body is required")
    String body,

    @NotNull(message = "Status is required")
    Status status
) {}