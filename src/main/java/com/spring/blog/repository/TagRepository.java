package com.spring.blog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.blog.model.TagModel;



public interface TagRepository extends JpaRepository<TagModel, Long> {
    Optional<TagModel>  findByName(String name);
    
} 
