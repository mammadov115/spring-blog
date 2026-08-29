package com.spring.blog.dto;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "urlset", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
@AllArgsConstructor 
@NoArgsConstructor 
public class SiteMap{
    private List<SitemapUrl> urls;
    
    @XmlElement(name = "url")
    public List<SitemapUrl> getUrls(){return  urls;}
}