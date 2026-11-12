package bd.bracuinsiders.model;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version private long version;
    private String slug;
    private String title;
    private String caption;

    @Column(columnDefinition = "text")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;

    private boolean published;
    private boolean featured;
    private Instant publishedAt;
    private Instant updatedAt;
    private String sourceUrl;
    private String videoUrl;
    private LocalDate eventDate;
    private LocalDate eventEnd;
    private String eventInfo;

    @ManyToMany
    @JoinTable(
            name = "post_media",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "media_id"))
    @OrderColumn(name = "position")
    private List<Media> images = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getCaption() {
        return caption;
    }

    public String getBody() {
        return body;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isFeatured() {
        return featured;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public LocalDate getEventEnd() {
        return eventEnd;
    }

    public String getEventInfo() {
        return eventInfo;
    }

    public List<Media> getImages() {
        return images;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void edit(
            String title,
            String caption,
            String body,
            Category category,
            String sourceUrl,
            String videoUrl,
            LocalDate eventDate,
            LocalDate eventEnd,
            String eventInfo,
            boolean featured,
            List<Media> pictures) {
        this.title = title;
        this.caption = caption;
        this.body = body;
        this.category = category;
        this.sourceUrl = sourceUrl;
        this.videoUrl = videoUrl;
        this.eventDate = eventDate;
        this.eventEnd = eventEnd;
        this.eventInfo = eventInfo;
        this.featured = featured;
        this.images.clear();
        this.images.addAll(pictures);
        this.updatedAt = Instant.now();
    }

    public void setPublication(boolean publish, Instant date) {
        this.published = publish;
        if (date != null) this.publishedAt = date;
        if (publish && publishedAt == null) publishedAt = Instant.now();
        updatedAt = Instant.now();
    }

    public void setFeatured(boolean pinned) {
        featured = pinned;
        updatedAt = Instant.now();
    }
}
