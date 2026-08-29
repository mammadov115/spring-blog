package com.spring.blog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


import com.spring.blog.dto.PostResponse;
import com.spring.blog.dto.SharePostRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sharePost(PostResponse post, SharePostRequest request) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(request.getRecipientEmail());
    message.setSubject(request.getSenderName() + " shared with you: " + post.title());
    message.setText(
        request.getSenderName() + " (" + request.getSenderEmail() + ") " +
        "share post with you:\n\n" +
        post.title() + "\n\n" +
        "http://localhost:8080/api/posts/" + post.slug() +
        (request.getComment() != null && !request.getComment().isBlank()
            ? "\n\nŞərh: " + request.getComment()
            : "")
    );
    mailSender.send(message);
}
}
