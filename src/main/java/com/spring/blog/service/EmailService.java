package com.spring.blog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.spring.blog.model.Post;
import com.spring.blog.dto.SharePostRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sharePost(Post post, SharePostRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(request.getRecipientEmail());
        message.setSubject(request.getSenderName() + " shared a post with you!");
        message.setText("Subject : " + post.getTitle() + "\n" + "Body : " + post.getBody() + "\n" + "Post URL : "
                + "http://localhost:8080/api/posts/" + post.getId());

        mailSender.send(message);
    }
}
