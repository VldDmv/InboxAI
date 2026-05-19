package com.inboxai.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SourceForm {

    @NotBlank(message = "URL is required")
    @Size(max = 2048)
    private String url;

    @Size(max = 512)
    private String title;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
