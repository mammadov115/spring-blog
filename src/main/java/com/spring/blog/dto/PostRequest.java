package com.spring.blog.dto;

import com.spring.blog.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating or updating a post")
public record PostRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or less")
    @Schema(description = "Title of the post", example = "My First Post")
    String title,

    @Size(max = 255, message = "Slug must be 255 characters or less")
    @Schema(
        description = "URL-friendly identifier. If empty or omitted, auto-generated from title. Example: 'My First Post' → 'my-first-post'",
        example = "my-first-post",
        nullable = true
    )
    String slug,

    @NotBlank(message = "Body is required")
    @Schema(description = "Main content of the post. Script tags and dangerous HTML are stripped automatically.", example = "This is the post content.")
    String body,

    @NotNull(message = "Status is required")
    @Schema(description = "Publication status", allowableValues = {"DRAFT", "PUBLISHED"}, example = "DRAFT")
    Status status
) {}
