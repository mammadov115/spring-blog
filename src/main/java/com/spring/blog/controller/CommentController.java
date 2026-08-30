package com.spring.blog.controller;

import com.spring.blog.service.CommentService;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.blog.dto.CommentRequest;
import com.spring.blog.dto.CommentResponse;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("api/posts/{id}/comments")
@Tag(name = "Comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "Get comments by post")
    public List<CommentResponse> getComments(@PathVariable Long id){
        return commentService.getCommentByPost(id);
    }

    @PostMapping
    @Operation(summary = "Add comment to post")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id, @RequestBody @Valid CommentRequest request) {
        CommentResponse saved = commentService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
}
