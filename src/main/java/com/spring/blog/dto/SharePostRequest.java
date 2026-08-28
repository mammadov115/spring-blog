package com.spring.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SharePostRequest {
    @NotBlank
    private String senderName;

    @NotBlank
    @Email
    private String senderEmail;

    @NotBlank
    @Email
    private String recipientEmail;

    private String comment;
}
