package com.spring.blog.dto;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "channel")
@AllArgsConstructor 
@NoArgsConstructor 
public class RssChannel {
    private String title;
    private String link;
    private String description;
    private List<RssItem> items;

    @XmlElement public String getTitle() { return title; }
    @XmlElement public String getLink() { return link; }
    @XmlElement public String getDescription() { return description; }
    @XmlElement(name = "item") public List<RssItem> getItems() { return items; }
}
