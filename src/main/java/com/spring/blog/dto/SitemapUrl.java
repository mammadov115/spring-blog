package com.spring.blog.dto;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "url")
@AllArgsConstructor 
@NoArgsConstructor 
public class SitemapUrl {
    private String loc;
    private String lastmod;
    private String changefreq;
    private String priority;

    @XmlElement
    public String getLoc() { return loc; }

    @XmlElement
    public String getLastmod() { return lastmod; }

    @XmlElement
    public String getChangefreq() { return changefreq; }

    @XmlElement
    public String getPriority() { return priority; }
}
