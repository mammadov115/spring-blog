package com.spring.blog.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.blog.model.TagModel;

public interface TagRepository extends JpaRepository<TagModel, Long> {
    Optional<TagModel> findByName(String name);
    Set<TagModel> findByNameIn(Set<String> names);
} 
