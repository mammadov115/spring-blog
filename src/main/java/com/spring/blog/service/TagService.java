package com.spring.blog.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.spring.blog.dto.PostResponse;
import com.spring.blog.exception.ResourceNotFoundException;
import com.spring.blog.model.Post;
import com.spring.blog.model.TagModel;
import com.spring.blog.repository.PostRepository;
import com.spring.blog.repository.TagRepository;
import com.spring.blog.util.PostMapper;

import org.springframework.transaction.annotation.Transactional;
import com.spring.blog.model.Status;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostMapper postMapper;

    @CacheEvict(value = "posts-by-tag", allEntries = true)
    @Transactional
    public PostResponse addTagsToPost(String slug, Set<String> tagNames) {
        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Set<String> validNames = tagNames.stream()
                .filter(name -> name != null && !name.isBlank() && name.length() <= 100)
                .collect(Collectors.toSet());

        Set<TagModel> existingTags = tagRepository.findByNameIn(validNames);
        Set<String> existingNames = existingTags.stream()
                .map(TagModel::getName)
                .collect(Collectors.toSet());

        Set<TagModel> newTags = validNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> tagRepository.save(TagModel.builder().name(name).build()))
                .collect(Collectors.toSet());

        Set<TagModel> tags = new java.util.HashSet<>();
        tags.addAll(existingTags);
        tags.addAll(newTags);

        post.getTags().addAll(tags);
        return postMapper.toResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Set<TagModel> getTagsByPost(String slug) {
        Post post = postRepository.findBySlugAndStatusWithTags(slug, Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + slug));
        return post.getTags();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByTag(String tagName) {
        TagModel tag = tagRepository.findByName(tagName)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagName));
        return postRepository.findByTagsContaining(tag)
                .stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());
    }

}
