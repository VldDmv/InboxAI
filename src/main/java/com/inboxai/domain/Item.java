package com.inboxai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_items_source_guid",
                columnNames = {"source_id", "guid"}
        ),
        indexes = {
                @Index(name = "idx_items_source_published", columnList = "source_id, published_at"),
                @Index(name = "idx_items_category", columnList = "category")
        }
)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(name = "guid", nullable = false, length = 1024)
    private String guid;

    @Column(name = "title", length = 1024)
    private String title;

    @Column(name = "link", length = 2048)
    private String link;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private Category category;

    @Column(name = "summary", length = 1024)
    private String summary;

    @Column(name = "classified_at")
    private Instant classifiedAt;

    @Column(name = "classification_cost_usd", precision = 12, scale = 8)
    private BigDecimal classificationCostUsd;

    protected Item() {
    }

    public Item(Source source, String guid, String title, String link,
                String contentHtml, String contentText, Instant publishedAt) {
        this.source = source;
        this.guid = guid;
        this.title = title;
        this.link = link;
        this.contentHtml = contentHtml;
        this.contentText = contentText;
        this.publishedAt = publishedAt;
        this.fetchedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getClassifiedAt() {
        return classifiedAt;
    }

    public void setClassifiedAt(Instant classifiedAt) {
        this.classifiedAt = classifiedAt;
    }

    public BigDecimal getClassificationCostUsd() {
        return classificationCostUsd;
    }

    public void setClassificationCostUsd(BigDecimal classificationCostUsd) {
        this.classificationCostUsd = classificationCostUsd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
