package com.spring.blog.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.blog.dto.SiteMap;
import com.spring.blog.dto.SitemapUrl;
import com.spring.blog.model.Status;
import com.spring.blog.repository.PostRepository;


import lombok.RequiredArgsConstructor;

@Service
@Cacheable(value = "sitemap")
@RequiredArgsConstructor
public class SiteMapService {
    private final PostRepository postRepository;

    @Value("${app.base-url}")
    private  String baseUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public SiteMap getSiteMap() {
        List<SitemapUrl> urls = postRepository
                .findByStatusOrderByPublishDesc(Status.PUBLISHED)
                .stream()
                .map(post -> new SitemapUrl(
                        baseUrl + "/api/posts/" + post.getSlug(),
                        post.getUpdated().format(FORMATTER),
                        "weekly",
                        "0.8"))
                .toList();
        return new SiteMap(urls);

    }
}