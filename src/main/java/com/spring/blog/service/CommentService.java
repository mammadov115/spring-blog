package com.spring.blog.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.spring.blog.dto.CommentRequest;
import com.spring.blog.dto.CommentResponse;
import com.spring.blog.exception.ResourceNotFoundException;
import com.spring.blog.model.Comment;
import com.spring.blog.model.Post;
import com.spring.blog.repository.CommentRepository;
import com.spring.blog.repository.PostRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<CommentResponse> getCommentByPost(Long postId) {
        return commentRepository
                .findByPostIdOrderByCreatedDesc(postId).stream().map(comment -> new CommentResponse(comment.getId(),
                        comment.getName(), comment.getEmail(), comment.getBody(), comment.getCreated()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + postId));

        Comment comment = Comment.builder()
                .post(post)
                .name(request.getName())
                .email(request.getEmail())
                .body(request.getBody())
                .build();

        Comment saved = commentRepository.save(comment);
        return new CommentResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getBody(), saved.getCreated());
    }
}
