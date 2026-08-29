package com.spring.blog.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.spring.blog.dto.RssChannel;
import com.spring.blog.dto.RssFeed;
import com.spring.blog.dto.RssItem;
import com.spring.blog.model.Status;
import com.spring.blog.repository.PostRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class RssService {
    private final PostRepository postRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss",
            Locale.ENGLISH);

    @Transactional(readOnly = true)
    public RssFeed getFeed() {
        List<RssItem> items = postRepository.findByStatusOrderByPublishDesc(Status.PUBLISHED, PageRequest.of(0, 20))
                .getContent().stream()
                .map(post -> new RssItem(post.getTitle(), baseUrl + "/api/posts/" + post.getSlug(),
                        post.getBody().length() > 200 ? post.getBody().substring(0, 200) + "..." : post.getBody(),
                        post.getPublish().format(FORMATTER), baseUrl + "/api/posts/" + post.getSlug()))
                .toList();

        RssChannel channel = new RssChannel("My Blog", baseUrl, "Brief about blog", items);

        return new RssFeed("2.0", channel);
    }

}
