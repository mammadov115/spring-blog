package com.spring.blog.dto;

import com.spring.blog.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or less")
    String title,

    @Size(max = 255, message = "Slug must be 255 characters or less")
    String slug,

    @NotBlank(message = "Body is required")
    String body,

    @NotNull(message = "Status is required")
    Status status
) {}
