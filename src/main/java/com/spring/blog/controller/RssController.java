package com.spring.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.blog.dto.RssFeed;
import com.spring.blog.service.RssService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
@Tag(name = "RSS Feed")
public class RssController {
    private final RssService rssService;

    @GetMapping(value = "/rss", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Get RSS feed")
    public ResponseEntity<RssFeed> getRssFeed() {
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=1800")
                .body(rssService.getFeed());
    }
}
