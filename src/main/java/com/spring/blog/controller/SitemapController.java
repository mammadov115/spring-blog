package com.spring.blog.controller;

import com.spring.blog.service.SiteMapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.blog.dto.SiteMap;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
@Tag(name = "Sitemap")
public class SitemapController {
    private final SiteMapService siteMapService;


    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Get sitemap")
    public SiteMap getSiteMap(){
        return siteMapService.getSiteMap();
    }



}
