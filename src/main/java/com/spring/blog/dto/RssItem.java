package com.spring.blog.dto;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "item")
@AllArgsConstructor
@NoArgsConstructor
public class RssItem {
    private String title;
    private String link;
    private String description;
    private String pubDate;
    private String guid;

    @XmlElement
    public String getTitle() {
        return title;
    }

    @XmlElement
    public String getLink() {
        return link;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    @XmlElement
    public String getPubDate() {
        return pubDate;
    }

    @XmlElement
    public String getGuid() {
        return guid;
    }

}
