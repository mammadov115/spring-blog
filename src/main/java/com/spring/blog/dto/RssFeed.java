package com.spring.blog.dto;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "rss")
@AllArgsConstructor 
@NoArgsConstructor 
public class RssFeed {
    private String version = "2.0";
    private RssChannel channel;

    @XmlAttribute public String getVersion() { return version; }
    @XmlElement public RssChannel getChannel() { return channel; }

}