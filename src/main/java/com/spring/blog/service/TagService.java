package com.spring.blog.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.spring.blog.exception.ResourceNotFoundException;
import com.spring.blog.model.Post;
import com.spring.blog.model.TagModel;
import com.spring.blog.repository.PostRepository;
import com.spring.blog.repository.TagRepository;

import jakarta.transaction.Transactional;

import com.spring.blog.model.Status;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class TagService {
    private final PostRepository postRepository;
    private  final TagRepository tagRepository;

    @Transactional 
    public Post addTagsToPost(String slug, Set<String> tagNames){
        Post post  = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED).orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Set<TagModel> tags = tagNames.stream().map(name -> tagRepository.findByName(name).orElseGet(() -> tagRepository.save(TagModel.builder().name(name).build()))).collect(Collectors.toSet());

        post.getTags().addAll(tags);
        return postRepository.save(post);
        
    }
    
    @Transactional 
    public Set<TagModel> getTagsByPost(String slug){
        Post post = postRepository.findBySlugAndStatus(slug, Status.PUBLISHED).orElseThrow(() -> new ResourceNotFoundException("Tag not found" + slug));
        return post.getTags();
    }


    public List<Post> getPostsByTag(String tagName){
        TagModel tag  = tagRepository.findByName(tagName).orElseThrow(() -> new ResourceNotFoundException("Tag not found" + tagName));

        return postRepository.findByTagsContaining(tag);

    }
}
