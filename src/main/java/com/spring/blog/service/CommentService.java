package com.spring.blog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.spring.blog.dto.CommentRequest;
import com.spring.blog.model.Comment;
import com.spring.blog.model.Post;
import com.spring.blog.repository.CommentRepository;
import com.spring.blog.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<Comment> getCommentByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedDesc(postId);
    }

    public Comment addComment(Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id " + postId));

        Comment comment = Comment.builder()
                .post(post)
                .name(request.getName())
                .email(request.getEmail())
                .body(request.getBody())
                .build();

        return commentRepository.save(comment);
    }
}
